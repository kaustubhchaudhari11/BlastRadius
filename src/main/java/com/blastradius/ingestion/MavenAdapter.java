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
 * <p>Parses the raw {@code <dependencies>} block of a pom.xml. Versions that are
 * inherited from a parent or managed by a BOM may be absent at this level; for the
 * MVP we record {@code "unspecified"} rather than resolving the full effective model.
 */
@Component
public class MavenAdapter implements EcosystemAdapter {

	private static final Logger log = LoggerFactory.getLogger(MavenAdapter.class);
	private static final String ECOSYSTEM = "maven";
	private static final String UNSPECIFIED_VERSION = "unspecified";

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

		List<ParsedDependency> result = new ArrayList<>();
		for (org.apache.maven.model.Dependency d : model.getDependencies()) {
			String version = (d.getVersion() == null || d.getVersion().isBlank())
					? UNSPECIFIED_VERSION
					: d.getVersion();
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
