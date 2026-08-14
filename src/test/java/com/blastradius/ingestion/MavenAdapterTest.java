package com.blastradius.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MavenAdapterTest {

	private final MavenAdapter adapter = new MavenAdapter((g, a, v) -> Optional.empty());

	private Path fixture() {
		return Path.of("src/test/resources/fixtures/maven-sample");
	}

	@Test
	void ecosystemIdIsMaven() {
		assertThat(adapter.ecosystemId()).isEqualTo("maven");
	}

	@Test
	void detectsRepoWithPom() {
		assertThat(adapter.detect(fixture())).isTrue();
	}

	@Test
	void doesNotDetectRepoWithoutPom() {
		assertThat(adapter.detect(Path.of("src/test/resources"))).isFalse();
	}

	@Test
	void parsesAllDeclaredDependencies() {
		List<ParsedDependency> deps = adapter.parseDependencies(fixture());

		assertThat(deps).hasSize(3);
		assertThat(deps).allMatch(d -> d.ecosystem().equals("maven"));
		assertThat(deps).contains(
				new ParsedDependency("maven", "com.google.guava", "guava", "32.1.2-jre"),
				new ParsedDependency("maven", "org.apache.commons", "commons-lang3", "3.12.0"));
	}

	@Test
	void recordsUnspecifiedWhenVersionAbsent() {
		List<ParsedDependency> deps = adapter.parseDependencies(fixture());

		assertThat(deps)
				.filteredOn(d -> d.artifactOrName().equals("spring-core"))
				.singleElement()
				.extracting(ParsedDependency::version)
				.isEqualTo("unspecified");
	}

	@Test
	void throwsOnMissingPom() {
		assertThatThrownBy(() -> adapter.parseDependencies(Path.of("src/test/resources")))
				.isInstanceOf(IngestionException.class);
	}

	@Test
	void scanUsageNotYetImplemented() {
		assertThatThrownBy(() -> adapter.scanUsage(fixture(), null))
				.isInstanceOf(UnsupportedOperationException.class);
	}
}
