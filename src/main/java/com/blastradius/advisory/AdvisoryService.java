package com.blastradius.advisory;

import com.blastradius.advisory.osv.OsvModels.Vulnerability;
import com.blastradius.model.Advisory;
import com.blastradius.model.AdvisoryRepository;
import com.blastradius.model.Dependency;
import com.blastradius.model.DependencyRepository;
import com.blastradius.model.ProjectRepository;
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
 *   <li><strong>Idempotent</strong> — advisories are deduped on {@code external_id}, so
 *       repeated refreshes update rather than duplicate.</li>
 *   <li><strong>Partially fault-tolerant</strong> — one package failing at OSV must not lose
 *       the advisories already gathered, so failures are collected per package and reported.</li>
 * </ul>
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

	public AdvisoryService(
			AdvisoryClient advisoryClient,
			AdvisoryMapper advisoryMapper,
			VersionRangeMatcher versionRangeMatcher,
			AdvisoryRepository advisoryRepository,
			DependencyRepository dependencyRepository,
			ProjectRepository projectRepository) {
		this.advisoryClient = advisoryClient;
		this.advisoryMapper = advisoryMapper;
		this.versionRangeMatcher = versionRangeMatcher;
		this.advisoryRepository = advisoryRepository;
		this.dependencyRepository = dependencyRepository;
		this.projectRepository = projectRepository;
	}

	@Transactional
	public AdvisoryRefreshResult refreshForProject(Long projectId) {
		if (projectId == null || !projectRepository.existsById(projectId)) {
			throw new AdvisoryException("No such project: " + projectId);
		}

		List<Dependency> dependencies = dependencyRepository.findByProjectId(projectId);
		if (dependencies.isEmpty()) {
			log.info("Project {} has no dependencies to refresh", projectId);
			return new AdvisoryRefreshResult(projectId, 0, 0, 0, 0, 0, 0, List.of());
		}

		// Collapse to distinct packages: the same artifact can appear more than once, and
		// each extra occurrence would be a wasted network call.
		Map<PackageKey, String> distinct = new LinkedHashMap<>();
		for (Dependency dependency : dependencies) {
			PackageKey key = PackageKey.of(dependency);
			distinct.putIfAbsent(key, dependency.getCurrentVersion());
		}

		int found = 0;
		int created = 0;
		int affected = 0;
		int notAffected = 0;
		int unknown = 0;
		List<String> errors = new ArrayList<>();

		for (Map.Entry<PackageKey, String> entry : distinct.entrySet()) {
			PackageKey key = entry.getKey();
			String currentVersion = entry.getValue();
			try {
				// Query without a version when it is unresolved, so we still learn what
				// advisories exist for the package and can flag them for manual review.
				String queryVersion = VersionRangeMatcher.isUnresolved(currentVersion) ? null : currentVersion;
				List<Vulnerability> vulns =
						advisoryClient.findVulnerabilities(key.ecosystem(), key.osvName(), queryVersion);

				for (Vulnerability vuln : vulns) {
					found++;
					if (upsert(vuln, key)) {
						created++;
					}
					switch (versionRangeMatcher.match(currentVersion, vuln.affectedOrEmpty())) {
						case AFFECTED -> affected++;
						case NOT_AFFECTED -> notAffected++;
						case UNKNOWN -> unknown++;
					}
				}
			}
			catch (AdvisoryException e) {
				log.warn("Advisory refresh failed for {}: {}", key.osvName(), e.getMessage());
				errors.add(key.osvName() + ": " + e.getMessage());
			}
		}

		log.info("Project {}: queried {} packages, {} advisories ({} new) — {} affected, {} not affected, {} unknown",
				projectId, distinct.size(), found, created, affected, notAffected, unknown);
		return new AdvisoryRefreshResult(
				projectId, distinct.size(), found, created, affected, notAffected, unknown, errors);
	}

	/**
	 * @return {@code true} when a new advisory row was inserted
	 */
	private boolean upsert(Vulnerability vuln, PackageKey key) {
		Optional<Advisory> existing = advisoryRepository.findByExternalId(vuln.id());
		if (existing.isPresent()) {
			advisoryMapper.updateInPlace(existing.get(), vuln);
			advisoryRepository.save(existing.get());
			return false;
		}
		advisoryRepository.save(advisoryMapper.toAdvisory(vuln, key.ecosystem(), key.osvName()));
		return true;
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
