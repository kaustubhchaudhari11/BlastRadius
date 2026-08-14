package com.blastradius.advisory;

import com.blastradius.advisory.osv.OsvModels.Vulnerability;
import com.blastradius.model.Advisory;
import com.blastradius.model.AdvisoryRepository;
import com.blastradius.model.Dependency;
import com.blastradius.model.DependencyRepository;
import com.blastradius.model.Finding;
import com.blastradius.model.FindingRepository;
import com.blastradius.model.Project;
import com.blastradius.model.ProjectRepository;
import com.blastradius.model.TriageStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pulls advisories for a project's dependencies and records which ones actually apply.
 *
 * <p>Two properties matter here:
 * <ul>
 *   <li><strong>Idempotent</strong> — advisories dedupe on {@code external_id} and findings on
 *       {@code (dependency_id, advisory_id)}, so repeated refreshes update rather than duplicate.</li>
 *   <li><strong>Partially fault-tolerant</strong> — one package failing at OSV must not lose
 *       the advisories already gathered, so failures are collected per package and reported.</li>
 * </ul>
 *
 * <p>The verdict is persisted as a {@link Finding}, not just counted: it is the row that Phase 4
 * (usage evidence) and Phase 5 (triage) refine, and the only queryable record of <em>why</em> a
 * dependency was flagged.
 */
@Service
public class AdvisoryService {

	private static final Logger log = LoggerFactory.getLogger(AdvisoryService.class);

	private final AdvisoryClient advisoryClient;
	private final AdvisoryMapper advisoryMapper;
	private final VersionRangeMatcher versionRangeMatcher;
	private final AdvisoryRepository advisoryRepository;
	private final DependencyRepository dependencyRepository;
	private final ProjectRepository projectRepository;
	private final FindingRepository findingRepository;

	public AdvisoryService(
			AdvisoryClient advisoryClient,
			AdvisoryMapper advisoryMapper,
			VersionRangeMatcher versionRangeMatcher,
			AdvisoryRepository advisoryRepository,
			DependencyRepository dependencyRepository,
			ProjectRepository projectRepository,
			FindingRepository findingRepository) {
		this.advisoryClient = advisoryClient;
		this.advisoryMapper = advisoryMapper;
		this.versionRangeMatcher = versionRangeMatcher;
		this.advisoryRepository = advisoryRepository;
		this.dependencyRepository = dependencyRepository;
		this.projectRepository = projectRepository;
		this.findingRepository = findingRepository;
	}

	@Transactional
	public AdvisoryRefreshResult refreshForProject(Long projectId) {
		if (projectId == null || !projectRepository.existsById(projectId)) {
			throw new AdvisoryException("No such project: " + projectId);
		}

		List<Dependency> dependencies = dependencyRepository.findByProjectId(projectId);
		if (dependencies.isEmpty()) {
			log.info("Project {} has no dependencies to refresh", projectId);
			return new AdvisoryRefreshResult(projectId, 0, 0, 0, 0, 0, 0, 0, List.of());
		}

		// Collapse to distinct packages so the same artifact is not fetched twice, but keep
		// every occurrence: each one needs its own finding.
		Map<PackageKey, List<Dependency>> byPackage = new LinkedHashMap<>();
		for (Dependency dependency : dependencies) {
			byPackage.computeIfAbsent(PackageKey.of(dependency), k -> new ArrayList<>()).add(dependency);
		}

		Project project = projectRepository.getReferenceById(projectId);
		Counters counters = new Counters();
		List<String> errors = new ArrayList<>();

		for (Map.Entry<PackageKey, List<Dependency>> entry : byPackage.entrySet()) {
			PackageKey key = entry.getKey();
			List<Dependency> occurrences = entry.getValue();
			try {
				// Query without a version when it is unresolved, so we still learn what
				// advisories exist for the package and can flag them for manual review.
				String representativeVersion = occurrences.get(0).getCurrentVersion();
				String queryVersion =
						VersionRangeMatcher.isUnresolved(representativeVersion) ? null : representativeVersion;
				List<Vulnerability> vulns =
						advisoryClient.findVulnerabilities(key.ecosystem(), key.osvName(), queryVersion);

				for (Vulnerability vuln : vulns) {
					counters.found++;
					Advisory advisory = upsertAdvisory(vuln, key, counters);
					for (Dependency dependency : occurrences) {
						VersionMatch verdict =
								versionRangeMatcher.match(dependency.getCurrentVersion(), vuln.affectedOrEmpty());
						counters.record(verdict);
						if (upsertFinding(project, dependency, advisory, verdict)) {
							counters.findingsCreated++;
						}
					}
				}
			}
			catch (AdvisoryException e) {
				log.warn("Advisory refresh failed for {}: {}", key.osvName(), e.getMessage());
				errors.add(key.osvName() + ": " + e.getMessage());
			}
		}

		log.info("Project {}: queried {} packages, {} advisories ({} new), {} findings ({} new)"
						+ " — {} affected, {} not affected, {} unknown",
				projectId, byPackage.size(), counters.found, counters.created,
				counters.affected + counters.notAffected + counters.unknown, counters.findingsCreated,
				counters.affected, counters.notAffected, counters.unknown);

		return new AdvisoryRefreshResult(
				projectId,
				byPackage.size(),
				counters.found,
				counters.created,
				counters.findingsCreated,
				counters.affected,
				counters.notAffected,
				counters.unknown,
				errors);
	}

	/** @return the persisted advisory, creating it when unseen. */
	private Advisory upsertAdvisory(Vulnerability vuln, PackageKey key, Counters counters) {
		Optional<Advisory> existing = advisoryRepository.findByExternalId(vuln.id());
		if (existing.isPresent()) {
			Advisory advisory = existing.get();
			advisoryMapper.updateInPlace(advisory, vuln);
			return advisoryRepository.save(advisory);
		}
		counters.created++;
		return advisoryRepository.save(advisoryMapper.toAdvisory(vuln, key.ecosystem(), key.osvName()));
	}

	/**
	 * @return {@code true} when a new finding row was inserted
	 */
	private boolean upsertFinding(
			Project project, Dependency dependency, Advisory advisory, VersionMatch verdict) {
		String status = toTriageStatus(verdict).code();
		Optional<Finding> existing =
				findingRepository.findByDependencyIdAndAdvisoryId(dependency.getId(), advisory.getId());
		if (existing.isPresent()) {
			Finding finding = existing.get();
			// Never overwrite a human decision with an automated verdict.
			if (!TriageStatus.DISMISSED.code().equals(finding.getTriageStatus())) {
				finding.setTriageStatus(status);
			}
			findingRepository.save(finding);
			return false;
		}
		findingRepository.save(Finding.builder()
				.project(project)
				.dependency(dependency)
				.advisory(advisory)
				.triageStatus(status)
				.createdAt(Instant.now())
				.build());
		return true;
	}

	private static TriageStatus toTriageStatus(VersionMatch verdict) {
		return switch (verdict) {
			case AFFECTED -> TriageStatus.AFFECTED;
			case NOT_AFFECTED -> TriageStatus.NOT_AFFECTED;
			case UNKNOWN -> TriageStatus.NEEDS_REVIEW;
		};
	}

	/** Mutable tally, kept local so the refresh loop stays readable. */
	private static final class Counters {
		private int found;
		private int created;
		private int findingsCreated;
		private int affected;
		private int notAffected;
		private int unknown;

		void record(VersionMatch verdict) {
			switch (verdict) {
				case AFFECTED -> affected++;
				case NOT_AFFECTED -> notAffected++;
				case UNKNOWN -> unknown++;
			}
		}
	}

	/** Identity of a package for OSV lookups. */
	private record PackageKey(String ecosystem, String osvName) {

		static PackageKey of(Dependency dependency) {
			return new PackageKey(dependency.getEcosystem(), osvName(dependency));
		}

		/** Maven identifies packages as {@code group:artifact}; others use the bare name. */
		private static String osvName(Dependency dependency) {
			if ("maven".equalsIgnoreCase(dependency.getEcosystem())) {
				return dependency.getGroupOrPkg() + ":" + dependency.getArtifactOrName();
			}
			return dependency.getArtifactOrName();
		}
	}
}
