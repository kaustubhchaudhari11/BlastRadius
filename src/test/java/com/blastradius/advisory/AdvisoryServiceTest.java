package com.blastradius.advisory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.blastradius.advisory.osv.OsvModels.Affected;
import com.blastradius.advisory.osv.OsvModels.Event;
import com.blastradius.advisory.osv.OsvModels.OsvPackage;
import com.blastradius.advisory.osv.OsvModels.Range;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdvisoryServiceTest {

	private static final Long PROJECT_ID = 1L;

	@Mock
	private AdvisoryClient advisoryClient;
	@Mock
	private AdvisoryRepository advisoryRepository;
	@Mock
	private DependencyRepository dependencyRepository;
	@Mock
	private ProjectRepository projectRepository;
	@Mock
	private FindingRepository findingRepository;

	private AdvisoryService service;

	@BeforeEach
	void setUp() {
		service = new AdvisoryService(
				advisoryClient,
				new AdvisoryMapper(new ObjectMapper()),
				new VersionRangeMatcher(),
				advisoryRepository,
				dependencyRepository,
				projectRepository,
				findingRepository);
	}

	/** Repositories return the entity they persisted, mirroring Spring Data. */
	private void stubPersistence() {
		when(projectRepository.getReferenceById(PROJECT_ID)).thenReturn(new Project());
		when(advisoryRepository.save(any(Advisory.class))).thenAnswer(i -> i.getArgument(0));
		when(findingRepository.findByDependencyIdAndAdvisoryId(any(), any()))
				.thenReturn(Optional.empty());
	}

	private static Dependency dependency(String group, String artifact, String version) {
		return Dependency.builder()
				.ecosystem("maven")
				.groupOrPkg(group)
				.artifactOrName(artifact)
				.currentVersion(version)
				.build();
	}

	private static Vulnerability vuln(String id, String introduced, String fixed) {
		return new Vulnerability(
				id, "summary", "details", null, List.of(),
				List.of(new Affected(
						new OsvPackage("com.google.guava:guava", "Maven"),
						List.of(new Range("ECOSYSTEM", List.of(
								new Event(introduced, null, null, null),
								new Event(null, fixed, null, null)))),
						List.of())),
				List.of());
	}

	private Finding capturedFinding() {
		ArgumentCaptor<Finding> captor = ArgumentCaptor.forClass(Finding.class);
		verify(findingRepository).save(captor.capture());
		return captor.getValue();
	}

	@Test
	void countsAffectedWhenCurrentVersionIsInVulnerableRange() {
		when(projectRepository.existsById(PROJECT_ID)).thenReturn(true);
		when(dependencyRepository.findByProjectId(PROJECT_ID))
				.thenReturn(List.of(dependency("com.google.guava", "guava", "31.0.0")));
		when(advisoryClient.findVulnerabilities("maven", "com.google.guava:guava", "31.0.0"))
				.thenReturn(List.of(vuln("GHSA-1", "0", "32.0.0")));
		when(advisoryRepository.findByExternalId("GHSA-1")).thenReturn(Optional.empty());
		stubPersistence();

		AdvisoryRefreshResult result = service.refreshForProject(PROJECT_ID);

		assertThat(result.affected()).isEqualTo(1);
		assertThat(result.notAffected()).isZero();
		assertThat(result.advisoriesCreated()).isEqualTo(1);
	}

	/** The verdict has to be persisted, not just counted — P4 and P5 read it back. */
	@Test
	void persistsFindingWithAffectedTriageStatus() {
		when(projectRepository.existsById(PROJECT_ID)).thenReturn(true);
		when(dependencyRepository.findByProjectId(PROJECT_ID))
				.thenReturn(List.of(dependency("com.google.guava", "guava", "31.0.0")));
		when(advisoryClient.findVulnerabilities("maven", "com.google.guava:guava", "31.0.0"))
				.thenReturn(List.of(vuln("GHSA-1", "0", "32.0.0")));
		when(advisoryRepository.findByExternalId("GHSA-1")).thenReturn(Optional.empty());
		stubPersistence();

		AdvisoryRefreshResult result = service.refreshForProject(PROJECT_ID);

		assertThat(result.findingsCreated()).isEqualTo(1);
		assertThat(capturedFinding().getTriageStatus()).isEqualTo(TriageStatus.AFFECTED.code());
		assertThat(capturedFinding().getCreatedAt()).isNotNull();
	}

	@Test
	void persistsUnresolvedVersionAsNeedsReview() {
		when(projectRepository.existsById(PROJECT_ID)).thenReturn(true);
		when(dependencyRepository.findByProjectId(PROJECT_ID))
				.thenReturn(List.of(dependency("com.google.guava", "guava", "unspecified")));
		when(advisoryClient.findVulnerabilities("maven", "com.google.guava:guava", null))
				.thenReturn(List.of(vuln("GHSA-1", "0", "32.0.0")));
		when(advisoryRepository.findByExternalId("GHSA-1")).thenReturn(Optional.empty());
		stubPersistence();

		service.refreshForProject(PROJECT_ID);

		assertThat(capturedFinding().getTriageStatus()).isEqualTo(TriageStatus.NEEDS_REVIEW.code());
	}

	@Test
	void updatesExistingFindingInsteadOfCreatingDuplicate() {
		when(projectRepository.existsById(PROJECT_ID)).thenReturn(true);
		when(dependencyRepository.findByProjectId(PROJECT_ID))
				.thenReturn(List.of(dependency("com.google.guava", "guava", "31.0.0")));
		when(advisoryClient.findVulnerabilities("maven", "com.google.guava:guava", "31.0.0"))
				.thenReturn(List.of(vuln("GHSA-1", "0", "32.0.0")));
		when(advisoryRepository.findByExternalId("GHSA-1")).thenReturn(Optional.empty());
		when(projectRepository.getReferenceById(PROJECT_ID)).thenReturn(new Project());
		when(advisoryRepository.save(any(Advisory.class))).thenAnswer(i -> i.getArgument(0));
		Finding stale = Finding.builder().triageStatus(TriageStatus.NOT_AFFECTED.code()).build();
		when(findingRepository.findByDependencyIdAndAdvisoryId(any(), any()))
				.thenReturn(Optional.of(stale));

		AdvisoryRefreshResult result = service.refreshForProject(PROJECT_ID);

		assertThat(result.findingsCreated()).isZero();
		assertThat(stale.getTriageStatus()).isEqualTo(TriageStatus.AFFECTED.code());
		verify(findingRepository).save(stale);
	}

	/** A human dismissal must survive an automated refresh. */
	@Test
	void doesNotOverwriteDismissedFinding() {
		when(projectRepository.existsById(PROJECT_ID)).thenReturn(true);
		when(dependencyRepository.findByProjectId(PROJECT_ID))
				.thenReturn(List.of(dependency("com.google.guava", "guava", "31.0.0")));
		when(advisoryClient.findVulnerabilities("maven", "com.google.guava:guava", "31.0.0"))
				.thenReturn(List.of(vuln("GHSA-1", "0", "32.0.0")));
		when(advisoryRepository.findByExternalId("GHSA-1")).thenReturn(Optional.empty());
		when(projectRepository.getReferenceById(PROJECT_ID)).thenReturn(new Project());
		when(advisoryRepository.save(any(Advisory.class))).thenAnswer(i -> i.getArgument(0));
		Finding dismissed = Finding.builder().triageStatus(TriageStatus.DISMISSED.code()).build();
		when(findingRepository.findByDependencyIdAndAdvisoryId(any(), any()))
				.thenReturn(Optional.of(dismissed));

		service.refreshForProject(PROJECT_ID);

		assertThat(dismissed.getTriageStatus()).isEqualTo(TriageStatus.DISMISSED.code());
	}

	@Test
	void filtersOutAdvisoryWhenVersionIsAlreadyPatched() {
		when(projectRepository.existsById(PROJECT_ID)).thenReturn(true);
		when(dependencyRepository.findByProjectId(PROJECT_ID))
				.thenReturn(List.of(dependency("com.google.guava", "guava", "33.0.0")));
		when(advisoryClient.findVulnerabilities("maven", "com.google.guava:guava", "33.0.0"))
				.thenReturn(List.of(vuln("GHSA-1", "0", "32.0.0")));
		when(advisoryRepository.findByExternalId("GHSA-1")).thenReturn(Optional.empty());
		stubPersistence();

		AdvisoryRefreshResult result = service.refreshForProject(PROJECT_ID);

		assertThat(result.notAffected()).isEqualTo(1);
		assertThat(result.affected()).isZero();
	}

	/** The P2.5 boundary: unresolved versions must be surfaced, not silently matched. */
	@Test
	void routesUnspecifiedVersionToUnknownAndQueriesWithoutVersion() {
		when(projectRepository.existsById(PROJECT_ID)).thenReturn(true);
		when(dependencyRepository.findByProjectId(PROJECT_ID))
				.thenReturn(List.of(dependency("com.google.guava", "guava", "unspecified")));
		when(advisoryClient.findVulnerabilities("maven", "com.google.guava:guava", null))
				.thenReturn(List.of(vuln("GHSA-1", "0", "32.0.0")));
		when(advisoryRepository.findByExternalId("GHSA-1")).thenReturn(Optional.empty());
		stubPersistence();

		AdvisoryRefreshResult result = service.refreshForProject(PROJECT_ID);

		assertThat(result.unknown()).isEqualTo(1);
		assertThat(result.affected()).isZero();
		assertThat(result.notAffected()).isZero();
		verify(advisoryClient).findVulnerabilities("maven", "com.google.guava:guava", null);
	}

	@Test
	void updatesInsteadOfDuplicatingWhenAdvisoryAlreadyKnown() {
		when(projectRepository.existsById(PROJECT_ID)).thenReturn(true);
		when(dependencyRepository.findByProjectId(PROJECT_ID))
				.thenReturn(List.of(dependency("com.google.guava", "guava", "31.0.0")));
		when(advisoryClient.findVulnerabilities(anyString(), anyString(), any()))
				.thenReturn(List.of(vuln("GHSA-1", "0", "32.0.0")));
		Advisory existing = Advisory.builder().externalId("GHSA-1").build();
		when(advisoryRepository.findByExternalId("GHSA-1")).thenReturn(Optional.of(existing));
		stubPersistence();

		AdvisoryRefreshResult result = service.refreshForProject(PROJECT_ID);

		assertThat(result.advisoriesCreated()).isZero();
		assertThat(result.advisoriesFound()).isEqualTo(1);
		verify(advisoryRepository).save(existing);
	}

	@Test
	void queriesEachDistinctPackageOnlyOnce() {
		when(projectRepository.existsById(PROJECT_ID)).thenReturn(true);
		when(dependencyRepository.findByProjectId(PROJECT_ID)).thenReturn(List.of(
				dependency("com.google.guava", "guava", "31.0.0"),
				dependency("com.google.guava", "guava", "31.0.0")));
		when(advisoryClient.findVulnerabilities(anyString(), anyString(), any()))
				.thenReturn(List.of());
		when(projectRepository.getReferenceById(PROJECT_ID)).thenReturn(new Project());

		AdvisoryRefreshResult result = service.refreshForProject(PROJECT_ID);

		assertThat(result.dependenciesQueried()).isEqualTo(1);
		verify(advisoryClient, times(1))
				.findVulnerabilities(eq("maven"), eq("com.google.guava:guava"), any());
	}

	/** One package appearing twice still needs a finding each, so nothing is under-reported. */
	@Test
	void createsAFindingPerDependencyOccurrence() {
		when(projectRepository.existsById(PROJECT_ID)).thenReturn(true);
		when(dependencyRepository.findByProjectId(PROJECT_ID)).thenReturn(List.of(
				dependency("com.google.guava", "guava", "31.0.0"),
				dependency("com.google.guava", "guava", "31.0.0")));
		when(advisoryClient.findVulnerabilities(anyString(), anyString(), any()))
				.thenReturn(List.of(vuln("GHSA-1", "0", "32.0.0")));
		when(advisoryRepository.findByExternalId("GHSA-1")).thenReturn(Optional.empty());
		stubPersistence();

		AdvisoryRefreshResult result = service.refreshForProject(PROJECT_ID);

		assertThat(result.dependenciesQueried()).isEqualTo(1);
		assertThat(result.findingsCreated()).isEqualTo(2);
		verify(findingRepository, times(2)).save(any(Finding.class));
	}

	/** One bad package must not abort the whole refresh. */
	@Test
	void collectsPerPackageErrorsAndKeepsGoing() {
		when(projectRepository.existsById(PROJECT_ID)).thenReturn(true);
		when(dependencyRepository.findByProjectId(PROJECT_ID)).thenReturn(List.of(
				dependency("org.example", "broken", "1.0.0"),
				dependency("com.google.guava", "guava", "31.0.0")));
		when(advisoryClient.findVulnerabilities("maven", "org.example:broken", "1.0.0"))
				.thenThrow(new AdvisoryException("OSV query failed"));
		when(advisoryClient.findVulnerabilities("maven", "com.google.guava:guava", "31.0.0"))
				.thenReturn(List.of(vuln("GHSA-1", "0", "32.0.0")));
		when(advisoryRepository.findByExternalId("GHSA-1")).thenReturn(Optional.empty());
		stubPersistence();

		AdvisoryRefreshResult result = service.refreshForProject(PROJECT_ID);

		assertThat(result.errors()).hasSize(1);
		assertThat(result.errors().get(0)).contains("org.example:broken");
		assertThat(result.affected()).isEqualTo(1);
	}

	@Test
	void returnsEmptyResultWhenProjectHasNoDependencies() {
		when(projectRepository.existsById(PROJECT_ID)).thenReturn(true);
		when(dependencyRepository.findByProjectId(PROJECT_ID)).thenReturn(List.of());

		AdvisoryRefreshResult result = service.refreshForProject(PROJECT_ID);

		assertThat(result.dependenciesQueried()).isZero();
		verify(advisoryClient, never()).findVulnerabilities(anyString(), anyString(), any());
		verify(findingRepository, never()).save(any(Finding.class));
	}
}
