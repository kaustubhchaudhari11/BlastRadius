package com.blastradius.advisory;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the OSV.dev advisory source.
 *
 * @param baseUrl        OSV API root, overridable so tests can point at a stub
 * @param connectTimeout fail fast when OSV is unreachable
 * @param readTimeout    advisory queries are a network hop; never block a request thread forever
 */
@ConfigurationProperties(prefix = "blastradius.osv")
public record OsvProperties(
		String baseUrl,
		Duration connectTimeout,
		Duration readTimeout) {

	public OsvProperties {
		if (baseUrl == null || baseUrl.isBlank()) {
			baseUrl = "https://api.osv.dev";
		}
		if (connectTimeout == null) {
			connectTimeout = Duration.ofSeconds(5);
		}
		if (readTimeout == null) {
			readTimeout = Duration.ofSeconds(15);
		}
	}
}
