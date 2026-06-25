package com.blastradius.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.blastradius.blastradius.BlastradiusApplication;
import com.blastradius.model.DependencyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Proves the full seam-to-DB path in CI: POST /api/projects parses the fixture pom via the
 * adapter, persists dependencies, and returns them. Runs against in-memory H2 (test profile).
 */
@SpringBootTest(classes = BlastradiusApplication.class)
@AutoConfigureMockMvc
class ProjectControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private DependencyRepository dependencyRepository;

	@Test
	void registersProjectAndPersistsMavenDependencies() throws Exception {
		long before = dependencyRepository.count();

		String body = """
				{
				  "name": "fixture-maven-sample",
				  "sourcePath": "src/test/resources/fixtures/maven-sample"
				}
				""";

		mockMvc.perform(post("/api/projects")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").exists())
				.andExpect(jsonPath("$.dependencies.length()").value(3))
				.andExpect(jsonPath("$.dependencies[0].ecosystem").value("maven"));

		org.assertj.core.api.Assertions.assertThat(dependencyRepository.count())
				.isGreaterThanOrEqualTo(before + 3);
	}

	@Test
	void rejectsInvalidSourcePath() throws Exception {
		String body = """
				{ "name": "bad", "sourcePath": "does/not/exist" }
				""";

		mockMvc.perform(post("/api/projects")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest());
	}
}
