package com.blastradius.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringReader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.junit.jupiter.api.Test;

/**
 * Proves the fix for the Phase 3 blocker: dependency versions that exist only in a remote
 * parent or an imported BOM must resolve to something concrete, otherwise advisory range
 * filtering degrades to "everything is unknown" on any Spring Boot–style project.
 *
 * <p>The remote repository is stubbed, so this runs offline and deterministically.
 */
class MavenRemoteVersionResolutionTest {

	/** Stands in for repo1.maven.org, serving POMs from an in-memory map. */
	private static final class StubPomFetcher implements PomFetcher {

		private final Map<String, String> poms = new HashMap<>();
		private int fetchCount;

		void put(String groupId, String artifactId, String version, String xml) {
			poms.put(groupId + ":" + artifactId + ":" + version, xml);
		}

		@Override
		public Optional<Model> fetch(String groupId, String artifactId, String version) {
			fetchCount++;
			String xml = poms.get(groupId + ":" + artifactId + ":" + version);
			if (xml == null) {
				return Optional.empty();
			}
			try {
				return Optional.of(new MavenXpp3Reader().read(new StringReader(xml)));
			}
			catch (Exception e) {
				return Optional.empty();
			}
		}
	}

	private static final String PARENT_POM = """
			<project>
			  <modelVersion>4.0.0</modelVersion>
			  <groupId>org.springframework.boot</groupId>
			  <artifactId>spring-boot-starter-parent</artifactId>
			  <version>3.4.5</version>
			  <parent>
			    <groupId>org.springframework.boot</groupId>
			    <artifactId>spring-boot-dependencies</artifactId>
			    <version>3.4.5</version>
			  </parent>
			</project>
			""";

	private static final String GRANDPARENT_POM = """
			<project>
			  <modelVersion>4.0.0</modelVersion>
			  <groupId>org.springframework.boot</groupId>
			  <artifactId>spring-boot-dependencies</artifactId>
			  <version>3.4.5</version>
			  <properties>
			    <postgresql.version>42.7.3</postgresql.version>
			  </properties>
			  <dependencyManagement>
			    <dependencies>
			      <dependency>
			        <groupId>org.springframework.boot</groupId>
			        <artifactId>spring-boot-starter-web</artifactId>
			        <version>3.4.5</version>
			      </dependency>
			      <dependency>
			        <groupId>org.postgresql</groupId>
			        <artifactId>postgresql</artifactId>
			        <version>${postgresql.version}</version>
			      </dependency>
			    </dependencies>
			  </dependencyManagement>
			</project>
			""";

	private static final String BOM_POM = """
			<project>
			  <modelVersion>4.0.0</modelVersion>
			  <groupId>com.example</groupId>
			  <artifactId>my-bom</artifactId>
			  <version>9.9.9</version>
			  <properties>
			    <guava.version>33.0.0-jre</guava.version>
			  </properties>
			  <dependencyManagement>
			    <dependencies>
			      <dependency>
			        <groupId>com.google.guava</groupId>
			        <artifactId>guava</artifactId>
			        <version>${guava.version}</version>
			      </dependency>
			    </dependencies>
			  </dependencyManagement>
			</project>
			""";

	private StubPomFetcher fetcher;

	private Map<String, String> resolve(String fixtureDir) {
		MavenAdapter adapter = new MavenAdapter(fetcher);
		List<ParsedDependency> deps =
				adapter.parseDependencies(Path.of("src/test/resources/fixtures/" + fixtureDir));
		return deps.stream().collect(
				Collectors.toMap(ParsedDependency::artifactOrName, ParsedDependency::version));
	}

	private StubPomFetcher fullStub() {
		StubPomFetcher stub = new StubPomFetcher();
		stub.put("org.springframework.boot", "spring-boot-starter-parent", "3.4.5", PARENT_POM);
		stub.put("org.springframework.boot", "spring-boot-dependencies", "3.4.5", GRANDPARENT_POM);
		stub.put("com.example", "my-bom", "9.9.9", BOM_POM);
		return stub;
	}

	@Test
	void resolvesVersionInheritedFromRemoteGrandparent() {
		fetcher = fullStub();
		assertThat(resolve("maven-remote-parent"))
				.containsEntry("spring-boot-starter-web", "3.4.5");
	}

	/** The grandparent manages this via its own ${property}, which must resolve too. */
	@Test
	void resolvesRemoteManagedVersionThatUsesAProperty() {
		fetcher = fullStub();
		assertThat(resolve("maven-remote-parent")).containsEntry("postgresql", "42.7.3");
	}

	@Test
	void resolvesVersionFromImportedBom() {
		fetcher = fullStub();
		assertThat(resolve("maven-remote-parent")).containsEntry("guava", "33.0.0-jre");
	}

	/** A locally declared version always wins over anything inherited or imported. */
	@Test
	void localExplicitVersionOverridesRemoteManagement() {
		fetcher = fullStub();
		assertThat(resolve("maven-remote-parent")).containsEntry("commons-lang3", "3.14.0");
	}

	/** Offline must degrade to "unspecified", never fail the whole ingest. */
	@Test
	void degradesGracefullyWhenRepositoryIsUnreachable() {
		fetcher = new StubPomFetcher(); // serves nothing, like being offline
		Map<String, String> versions = resolve("maven-remote-parent");

		assertThat(versions).containsEntry("spring-boot-starter-web", "unspecified");
		assertThat(versions).containsEntry("commons-lang3", "3.14.0"); // local version still fine
	}

	@Test
	void cachesSoTheSameParentIsNotFetchedRepeatedly() {
		fetcher = fullStub();
		resolve("maven-remote-parent");

		// parent + grandparent + bom = 3 distinct coordinates; anything much higher means
		// we would be hammering the repository during a real ingest.
		assertThat(fetcher.fetchCount).isLessThanOrEqualTo(4);
	}
}
