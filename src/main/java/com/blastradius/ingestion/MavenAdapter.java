package com.blastradius.ingestion;

import com.blastradius.model.Dependency;
import com.blastradius.model.UsageSite;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Maven/Java implementation of the ecosystem seam.
 *
 * <p>Parses the {@code <dependencies>} block of a pom.xml and resolves each version via
 * {@link MavenVersionResolver}: literal versions, {@code ${property}} placeholders, and
 * {@code <dependencyManagement>} entries (including those inherited from on-disk parent
 * POMs). A concrete version matters because P3 filters advisories by whether the current
 * version is in the vulnerable range.
 *
 * <p>Remote parents and imported BOMs (e.g. {@code spring-boot-dependencies}) are fetched via
 * {@link PomFetcher}. Anything still unresolved after that is recorded as
 * {@code "unspecified"} so the consumer can surface it for manual review.
 */
@Component
public class MavenAdapter implements EcosystemAdapter {

	private static final Logger log = LoggerFactory.getLogger(MavenAdapter.class);
	private static final String ECOSYSTEM = "maven";
	private static final String UNSPECIFIED_VERSION = "unspecified";

	private final PomFetcher pomFetcher;

	public MavenAdapter(PomFetcher pomFetcher) {
		this.pomFetcher = pomFetcher;
	}

	@Override
	public String ecosystemId() {
		return ECOSYSTEM;
	}

	@Override
	public boolean detect(Path repoRoot) {
		return repoRoot != null && Files.isRegularFile(repoRoot.resolve("pom.xml"));
	}

	@Override
	public List<ParsedDependency> parseDependencies(Path repoRoot) {
		Path pom = repoRoot.resolve("pom.xml");
		if (!Files.isRegularFile(pom)) {
			throw new IngestionException("No pom.xml found at " + pom);
		}

		Model model;
		try (InputStream in = Files.newInputStream(pom)) {
			model = new MavenXpp3Reader().read(in);
		}
		catch (IOException | XmlPullParserException e) {
			throw new IngestionException("Failed to parse pom.xml at " + pom, e);
		}

		MavenVersionResolver resolver = new MavenVersionResolver(model, repoRoot, pomFetcher);
		List<ParsedDependency> result = new ArrayList<>();
		for (org.apache.maven.model.Dependency d : model.getDependencies()) {
			String version = resolver.resolve(d.getVersion(), d.getGroupId(), d.getArtifactId())
					.orElse(UNSPECIFIED_VERSION);
			result.add(new ParsedDependency(ECOSYSTEM, d.getGroupId(), d.getArtifactId(), version));
		}
		log.info("Parsed {} maven dependencies from {}", result.size(), pom);
		return result;
	}

	@Override
	public List<UsageSite> scanUsage(Path repoRoot, Dependency dependency) {
		// Implemented in Phase 4 (import/symbol-level usage detection).
		throw new UnsupportedOperationException("scanUsage is implemented in Phase 4");
	}
}
