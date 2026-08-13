package com.blastradius.advisory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.blastradius.advisory.osv.OsvModels.Vulnerability;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Verifies the OSV request shape and response parsing without touching the network. */
class AdvisoryClientTest {

	private static final String OSV_RESPONSE = """
			{
			  "vulns": [
			    {
			      "id": "GHSA-7rjr-3q55-vv33",
			      "summary": "Deserialization flaw",
			      "published": "2022-03-30T00:00:00Z",
			      "severity": [{"type": "CVSS_V3", "score": "9.8"}],
			      "affected": [
			        {
			          "package": {"ecosystem": "Maven", "name": "com.google.guava:guava"},
			          "ranges": [
			            {"type": "ECOSYSTEM", "events": [{"introduced": "0"}, {"fixed": "32.0.0"}]}
			          ]
			        }
			      ]
			    }
			  ]
			}
			""";

	private RestClient restClient;
	private MockRestServiceServer server;
	private AdvisoryClient client;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://api.osv.dev");
		server = MockRestServiceServer.bindTo(builder).build();
		restClient = builder.build();
		client = new AdvisoryClient(restClient);
	}

	@Test
	void sendsOsvEcosystemNameAndParsesVulnerabilities() {
		server.expect(requestTo("https://api.osv.dev/v1/query"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(jsonPath("$.package.ecosystem").value("Maven"))
				.andExpect(jsonPath("$.package.name").value("com.google.guava:guava"))
				.andExpect(jsonPath("$.version").value("31.1-jre"))
				.andRespond(withSuccess(OSV_RESPONSE, MediaType.APPLICATION_JSON));

		List<Vulnerability> vulns =
				client.findVulnerabilities("maven", "com.google.guava:guava", "31.1-jre");

		assertThat(vulns).hasSize(1);
		Vulnerability vuln = vulns.get(0);
		assertThat(vuln.id()).isEqualTo("GHSA-7rjr-3q55-vv33");
		assertThat(vuln.summary()).isEqualTo("Deserialization flaw");
		assertThat(vuln.affectedOrEmpty()).hasSize(1);
		assertThat(vuln.affectedOrEmpty().get(0).rangesOrEmpty().get(0).eventsOrEmpty())
				.hasSize(2);
		server.verify();
	}

	@Test
	void returnsEmptyListWhenOsvKnowsNoVulnerabilities() {
		server.expect(requestTo("https://api.osv.dev/v1/query"))
				.andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

		assertThat(client.findVulnerabilities("maven", "org.example:safe", "1.0.0")).isEmpty();
	}

	@Test
	void wrapsHttpFailureInAdvisoryException() {
		server.expect(requestTo("https://api.osv.dev/v1/query")).andRespond(withServerError());

		assertThatThrownBy(() -> client.findVulnerabilities("maven", "org.example:lib", "1.0.0"))
				.isInstanceOf(AdvisoryException.class)
				.hasMessageContaining("OSV query failed");
	}

	@Test
	void rejectsUnmappedEcosystem() {
		assertThatThrownBy(() -> client.findVulnerabilities("cargo", "serde", "1.0.0"))
				.isInstanceOf(AdvisoryException.class)
				.hasMessageContaining("No OSV ecosystem mapping");
	}

	@Test
	void mapsKnownEcosystemNames() {
		assertThat(AdvisoryClient.toOsvEcosystem("maven")).isEqualTo("Maven");
		assertThat(AdvisoryClient.toOsvEcosystem("pypi")).isEqualTo("PyPI");
	}
}
