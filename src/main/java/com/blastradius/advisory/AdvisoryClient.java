package com.blastradius.advisory;

import com.blastradius.advisory.osv.OsvModels.OsvPackage;
import com.blastradius.advisory.osv.OsvModels.Query;
import com.blastradius.advisory.osv.OsvModels.QueryResponse;
import com.blastradius.advisory.osv.OsvModels.Vulnerability;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Reads advisories from OSV.dev.
 *
 * <p>Uses {@code POST /v1/query} (one call per package) rather than {@code /v1/querybatch}:
 * querybatch returns only vulnerability <em>ids</em>, so it would still require a follow-up
 * detail fetch per id. Since we need summary, severity, and affected ranges to populate an
 * {@code Advisory}, a single {@code /v1/query} per package is the same number of round trips
 * with simpler code.
 *
 * <p>Failures are surfaced as {@link AdvisoryException} so a refresh can report partial
 * success instead of leaving the caller with a raw HTTP error.
 */
@Component
public class AdvisoryClient {

	private static final Logger log = LoggerFactory.getLogger(AdvisoryClient.class);

	/** Blast Radius ecosystem id → OSV ecosystem name. */
	private static final Map<String, String> ECOSYSTEM_NAMES = Map.of(
			"maven", "Maven",
			"pypi", "PyPI",
			"npm", "npm");

	private final RestClient osvRestClient;

	public AdvisoryClient(RestClient osvRestClient) {
		this.osvRestClient = osvRestClient;
	}

	/**
	 * @param ecosystem  Blast Radius ecosystem id, e.g. {@code "maven"}
	 * @param pkgName    OSV package name; for Maven this is {@code group:artifact}
	 * @param version    concrete version, or {@code null} to ask for all advisories on the package
	 * @return advisories OSV knows about, never {@code null}
	 */
	public List<Vulnerability> findVulnerabilities(String ecosystem, String pkgName, String version) {
		String osvEcosystem = toOsvEcosystem(ecosystem);
		Query body = new Query(new OsvPackage(pkgName, osvEcosystem), version);
		try {
			QueryResponse response = osvRestClient.post()
					.uri("/v1/query")
					.body(body)
					.retrieve()
					.body(QueryResponse.class);
			List<Vulnerability> vulns = response == null ? List.of() : response.vulnsOrEmpty();
			log.debug("OSV returned {} advisories for {}:{} @ {}", vulns.size(), osvEcosystem, pkgName, version);
			return vulns;
		}
		catch (RestClientException e) {
			throw new AdvisoryException(
					"OSV query failed for " + osvEcosystem + ":" + pkgName + " @ " + version, e);
		}
	}

	static String toOsvEcosystem(String ecosystem) {
		if (ecosystem == null || ecosystem.isBlank()) {
			throw new AdvisoryException("Ecosystem must not be blank");
		}
		String mapped = ECOSYSTEM_NAMES.get(ecosystem.toLowerCase());
		if (mapped == null) {
			throw new AdvisoryException("No OSV ecosystem mapping for '" + ecosystem + "'");
		}
		return mapped;
	}
}
