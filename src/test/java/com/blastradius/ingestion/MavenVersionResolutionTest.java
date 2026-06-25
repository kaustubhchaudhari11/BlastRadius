package com.blastradius.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Covers P2.5 version resolution: ${property}, dependencyManagement, built-in
 * ${project.version}, and the unresolved fallback to "unspecified".
 */
class MavenVersionResolutionTest {

	private final MavenAdapter adapter = new MavenAdapter();

	private Map<String, String> versionsByArtifact() {
		List<ParsedDependency> deps =
				adapter.parseDependencies(Path.of("src/test/resources/fixtures/maven-props"));
		return deps.stream().collect(
				Collectors.toMap(ParsedDependency::artifactOrName, ParsedDependency::version));
	}

	@Test
	void resolvesPropertyPlaceholder() {
		assertThat(versionsByArtifact()).containsEntry("guava", "33.0.0-jre");
	}

	@Test
	void resolvesVersionFromDependencyManagement() {
		assertThat(versionsByArtifact()).containsEntry("jackson-databind", "2.17.1");
	}

	@Test
	void resolvesBuiltInProjectVersion() {
		assertThat(versionsByArtifact()).containsEntry("props-app-core", "2.5.0");
	}

	@Test
	void fallsBackToUnspecifiedWhenPropertyMissing() {
		assertThat(versionsByArtifact()).containsEntry("mystery", "unspecified");
	}
}
