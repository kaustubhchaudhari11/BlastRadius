package com.blastradius.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.blastradius.advisory.AdvisoryClient;
import com.blastradius.advisory.osv.OsvModels.Affected;
import com.blastradius.advisory.osv.OsvModels.Event;
import com.blastradius.advisory.osv.OsvModels.OsvPackage;
import com.blastradius.advisory.osv.OsvModels.Range;
import com.blastradius.advisory.osv.OsvModels.Vulnerability;
import com.blastradius.blastradius.BlastradiusApplication;
import com.blastradius.model.AdvisoryRepository;
import com.blastradius.model.Finding;
import com.blastradius.model.FindingRepository;
import com.blastradius.model.TriageStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * End-to-end proof of the Phase 3 path in CI: POST a project, refresh advisories, and confirm
 * rows land in the database with the correct tri-state verdicts.
 *
 * <p>{@link AdvisoryClient} is stubbed so the test never touches the network — the real HTTP
 * request/response handling is covered separately by {@code AdvisoryClientTest}.
 */
@SpringBootTest(classes = BlastradiusApplication.class)
@AutoConfigureMockMvc
class AdvisoryRefreshIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AdvisoryRepository advisoryRepository;

	@Autowired
	private FindingRepository findingRepository;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private AdvisoryClient advisoryClient;

	private static Vulnerability vuln(String id, String pkg, String introduced, String fixed) {
		return new Vulnerability(
				id, "summary of " + id, "details", null,
				List.of(),
				List.of(new Affected(
						new OsvPackage(pkg, "Maven"),
						List.of(new Range("ECOSYSTEM", List.of(
								new Event(introduced, null, null, null),
								new Event(null, fixed, null, null)))),
						List.of())),
				List.of());
	}

	private Long registerFixtureProject() throws Exception {
		String body = """
				{
				  "name": "advisory-fixture",
				  "sourcePath": "src/test/resources/fixtures/maven-sample"
				}
				""";
		String json = mockMvc.perform(post("/api/projects")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		JsonNode node = objectMapper.readTree(json);
		return node.get("id").asLong();
	}

	@Test
	void refreshPersistsAdvisoriesAndClassifiesEachVersion() throws Exception {
		Long projectId = registerFixtureProject();
		long advisoriesBefore = advisoryRepository.count();

		// Fixture pom holds guava 32.1.2-jre, commons-lang3 3.12.0, spring-core (no version).
		Mockito.when(advisoryClient.findVulnerabilities(
						"maven", "com.google.guava:guava", "32.1.2-jre"))
				.thenReturn(List.of(vuln("OSV-NOT-AFFECTED", "com.google.guava:guava", "0", "32.0.0")));
		Mockito.when(advisoryClient.findVulnerabilities(
						"maven", "org.apache.commons:commons-lang3", "3.12.0"))
				.thenReturn(List.of(vuln("OSV-AFFECTED", "org.apache.commons:commons-lang3", "0", "3.13.0")));
		// Unresolved version → queried with null version, verdict must be UNKNOWN.
		Mockito.when(advisoryClient.findVulnerabilities(
						Mockito.eq("maven"), Mockito.eq("org.springframework:spring-core"), isNull()))
				.thenReturn(List.of(vuln("OSV-UNKNOWN", "org.springframework:spring-core", "0", "6.0.0")));

		mockMvc.perform(post("/api/projects/{id}/advisories/refresh", projectId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.projectId").value(projectId))
				.andExpect(jsonPath("$.dependenciesQueried").value(3))
				.andExpect(jsonPath("$.advisoriesFound").value(3))
				.andExpect(jsonPath("$.advisoriesCreated").value(3))
				.andExpect(jsonPath("$.findingsCreated").value(3))
				.andExpect(jsonPath("$.affected").value(1))
				.andExpect(jsonPath("$.notAffected").value(1))
				.andExpect(jsonPath("$.unknown").value(1))
				.andExpect(jsonPath("$.errors").isEmpty());

		assertThat(advisoryRepository.count()).isEqualTo(advisoriesBefore + 3);
		assertThat(advisoryRepository.findByExternalId("OSV-AFFECTED")).isPresent();
		assertThat(advisoryRepository.findByExternalId("OSV-AFFECTED").orElseThrow().getSource())
				.isEqualTo("osv");

		// The verdict must be queryable, not just present in the HTTP response.
		List<Finding> findings = findingRepository.findByProjectId(projectId);
		assertThat(findings).hasSize(3);
		assertThat(findings).extracting(Finding::getTriageStatus)
				.containsExactlyInAnyOrder(
						TriageStatus.AFFECTED.code(),
						TriageStatus.NOT_AFFECTED.code(),
						TriageStatus.NEEDS_REVIEW.code());
		// Compare identifiers: the association is lazy and there is no session out here.
		Long affectedAdvisoryId =
				advisoryRepository.findByExternalId("OSV-AFFECTED").orElseThrow().getId();
		assertThat(findingRepository.findByProjectIdAndTriageStatus(
						projectId, TriageStatus.AFFECTED.code()))
				.singleElement()
				.satisfies(f -> assertThat(f.getAdvisory().getId()).isEqualTo(affectedAdvisoryId));
	}

	/** A second refresh must update, not duplicate — proves idempotency against a real DB. */
	@Test
	void repeatedRefreshDoesNotDuplicateAdvisoryRows() throws Exception {
		Long projectId = registerFixtureProject();

		Mockito.when(advisoryClient.findVulnerabilities(
						Mockito.anyString(), Mockito.anyString(), Mockito.any()))
				.thenReturn(List.of(vuln("OSV-IDEMPOTENT", "com.google.guava:guava", "0", "99.0.0")));

		mockMvc.perform(post("/api/projects/{id}/advisories/refresh", projectId))
				.andExpect(status().isOk());
		long afterFirst = advisoryRepository.count();
		int findingsAfterFirst = findingRepository.findByProjectId(projectId).size();

		mockMvc.perform(post("/api/projects/{id}/advisories/refresh", projectId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.advisoriesCreated").value(0))
				.andExpect(jsonPath("$.findingsCreated").value(0));

		assertThat(advisoryRepository.count()).isEqualTo(afterFirst);
		assertThat(findingRepository.findByProjectId(projectId)).hasSize(findingsAfterFirst);
	}

	@Test
	void refreshRejectsUnknownProject() throws Exception {
		mockMvc.perform(post("/api/projects/{id}/advisories/refresh", 999999))
				.andExpect(status().isBadRequest());
	}
}
