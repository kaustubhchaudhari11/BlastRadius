package com.blastradius.ingestion;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Fetches POMs from a Maven repository over HTTP so versions declared only in a remote parent
 * or BOM can still be resolved.
 *
 * <p>This is what makes advisory range-filtering work on Spring Boot–style projects: their
 * dependency versions live in {@code spring-boot-dependencies}, which is never on disk. Without
 * it, most dependencies resolve to {@code "unspecified"} and every advisory becomes
 * "needs manual review".
 *
 * <p>Results are cached per JVM (including negative lookups) because a single ingest asks for
 * the same parent repeatedly, and BOM POMs are immutable for a released version.
 */
@Component
public class MavenCentralPomFetcher implements PomFetcher {

	private static final Logger log = LoggerFactory.getLogger(MavenCentralPomFetcher.class);

	private final Map<String, Optional<Model>> cache = new ConcurrentHashMap<>();
	private final HttpClient httpClient;
	private final String repositoryBaseUrl;
	private final Duration requestTimeout;

	public MavenCentralPomFetcher(
			@Value("${blastradius.maven.repository-url:https://repo1.maven.org/maven2}") String repositoryBaseUrl,
			@Value("${blastradius.maven.request-timeout:10s}") Duration requestTimeout) {
		this.repositoryBaseUrl = repositoryBaseUrl.endsWith("/")
				? repositoryBaseUrl.substring(0, repositoryBaseUrl.length() - 1)
				: repositoryBaseUrl;
		this.requestTimeout = requestTimeout;
		this.httpClient = HttpClient.newBuilder()
				.connectTimeout(requestTimeout)
				.followRedirects(HttpClient.Redirect.NORMAL)
				.build();
	}

	@Override
	public Optional<Model> fetch(String groupId, String artifactId, String version) {
		if (isBlank(groupId) || isBlank(artifactId) || isBlank(version)) {
			return Optional.empty();
		}
		String key = groupId + ":" + artifactId + ":" + version;
		return cache.computeIfAbsent(key, k -> download(groupId, artifactId, version));
	}

	private Optional<Model> download(String groupId, String artifactId, String version) {
		String url = "%s/%s/%s/%s/%s-%s.pom".formatted(
				repositoryBaseUrl, groupId.replace('.', '/'), artifactId, version, artifactId, version);
		try {
			HttpRequest request = HttpRequest.newBuilder(URI.create(url))
					.timeout(requestTimeout)
					.GET()
					.build();
			HttpResponse<byte[]> response =
					httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
			if (response.statusCode() != 200) {
				log.debug("POM fetch returned {} for {}", response.statusCode(), url);
				return Optional.empty();
			}
			try (InputStream in = new ByteArrayInputStream(response.body())) {
				return Optional.of(new MavenXpp3Reader().read(in));
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.debug("POM fetch interrupted for {}", url);
			return Optional.empty();
		}
		catch (Exception e) {
			// Never fail ingestion because a remote lookup did not work; the version simply
			// stays unresolved and is reported as "unspecified".
			log.debug("POM fetch failed for {}: {}", url, e.getMessage());
			return Optional.empty();
		}
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
