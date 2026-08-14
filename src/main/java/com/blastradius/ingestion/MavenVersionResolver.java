package com.blastradius.ingestion;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves Maven dependency versions that are not stated literally on the dependency.
 *
 * <p>P3 filters advisories by whether the project's <em>current</em> version falls in a
 * vulnerable range, so a concrete version is load-bearing. A raw {@code <dependency>} may
 * express its version four indirect ways, all handled here:
 * <ul>
 *   <li>a {@code ${property}} placeholder defined in {@code <properties>} (this pom or any ancestor);</li>
 *   <li>no version at all, with the value supplied by {@code <dependencyManagement>};</li>
 *   <li>inheritance from a parent POM — on disk <em>or</em> fetched from a remote repository;</li>
 *   <li>an imported BOM ({@code <scope>import</scope>}), e.g. {@code spring-boot-dependencies}.</li>
 * </ul>
 *
 * <p>Precedence follows Maven: a child's own {@code dependencyManagement} beats an imported
 * BOM, which beats anything inherited from a parent.
 *
 * <p>Remote lookups go through {@link PomFetcher}, so resolution still works offline — it just
 * resolves less, and unresolved versions are reported as {@code "unspecified"} for manual review.
 */
class MavenVersionResolver {

	private static final Logger log = LoggerFactory.getLogger(MavenVersionResolver.class);
	private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}]+)}");
	private static final int MAX_DEPTH = 10;
	private static final String SCOPE_IMPORT = "import";
	private static final String TYPE_POM = "pom";

	private final Map<String, String> properties = new HashMap<>();
	private final Map<String, String> managedVersions = new HashMap<>();
	private final PomFetcher pomFetcher;

	MavenVersionResolver(Model rootModel, Path rootPomDir, PomFetcher pomFetcher) {
		this.pomFetcher = pomFetcher;
		accumulate(rootModel, rootPomDir, 0);
	}

	/**
	 * @return the fully concrete version, or empty if it cannot be resolved.
	 */
	Optional<String> resolve(String rawVersion, String groupId, String artifactId) {
		String candidate = (rawVersion == null || rawVersion.isBlank())
				? managedVersions.get(key(groupId, artifactId))
				: rawVersion;
		if (candidate == null || candidate.isBlank()) {
			return Optional.empty();
		}
		String resolved = substitute(candidate, 0);
		if (resolved == null || resolved.isBlank() || PLACEHOLDER.matcher(resolved).find()) {
			return Optional.empty();
		}
		return Optional.of(resolved);
	}

	/**
	 * Walks ancestors first so that nearer definitions overwrite inherited ones, then applies
	 * imported BOMs, then this model's explicit management — lowest to highest precedence.
	 */
	private void accumulate(Model model, Path pomDir, int depth) {
		if (model == null || depth > MAX_DEPTH) {
			return;
		}

		loadParent(model, pomDir, depth)
				.ifPresent(parent -> accumulate(parent.model(), parent.dir(), depth + 1));

		model.getProperties().forEach((k, v) -> properties.put(String.valueOf(k), String.valueOf(v)));
		registerBuiltInProperties(model);

		if (model.getDependencyManagement() != null) {
			// Imported BOMs first so explicit local entries can override them.
			for (org.apache.maven.model.Dependency managed : model.getDependencyManagement().getDependencies()) {
				if (isBomImport(managed)) {
					importBom(managed, depth);
				}
			}
			for (org.apache.maven.model.Dependency managed : model.getDependencyManagement().getDependencies()) {
				if (!isBomImport(managed) && managed.getVersion() != null && !managed.getVersion().isBlank()) {
					managedVersions.put(key(managed.getGroupId(), managed.getArtifactId()), managed.getVersion());
				}
			}
		}
	}

	/** A parent POM plus the directory to resolve <em>its</em> relative paths against. */
	private record ParentPom(Model model, Path dir) {
	}

	private Optional<ParentPom> loadParent(Model model, Path pomDir, int depth) {
		if (model.getParent() == null || depth >= MAX_DEPTH) {
			return Optional.empty();
		}

		Path localPom = localParentPom(model, pomDir);
		if (localPom != null && Files.isRegularFile(localPom)) {
			try (InputStream in = Files.newInputStream(localPom)) {
				return Optional.of(new ParentPom(new MavenXpp3Reader().read(in), localPom.getParent()));
			}
			catch (Exception e) {
				log.debug("Could not read parent pom at {}: {}", localPom, e.getMessage());
			}
		}

		// Not on disk (the common case for spring-boot-starter-parent): fetch it.
		String parentVersion = substitute(model.getParent().getVersion(), 0);
		return pomFetcher
				.fetch(model.getParent().getGroupId(), model.getParent().getArtifactId(), parentVersion)
				.map(remote -> new ParentPom(remote, null));
	}

	/**
	 * Resolves an imported BOM using the BOM's <em>own</em> properties, then merges only the
	 * concrete versions. BOM properties are deliberately not copied into this project's
	 * property map, matching Maven's behaviour.
	 */
	private void importBom(org.apache.maven.model.Dependency bomDependency, int depth) {
		if (depth >= MAX_DEPTH) {
			return;
		}
		String bomVersion = substitute(bomDependency.getVersion(), 0);
		if (bomVersion == null || PLACEHOLDER.matcher(bomVersion).find()) {
			return;
		}
		pomFetcher.fetch(bomDependency.getGroupId(), bomDependency.getArtifactId(), bomVersion)
				.ifPresent(bom -> {
					MavenVersionResolver bomResolver = new MavenVersionResolver(bom, null, pomFetcher);
					bomResolver.managedVersions.forEach((coordinate, version) -> {
						String concrete = bomResolver.substitute(version, 0);
						if (concrete != null && !PLACEHOLDER.matcher(concrete).find()) {
							managedVersions.put(coordinate, concrete);
						}
					});
				});
	}

	private static boolean isBomImport(org.apache.maven.model.Dependency dependency) {
		return SCOPE_IMPORT.equalsIgnoreCase(dependency.getScope())
				&& TYPE_POM.equalsIgnoreCase(dependency.getType());
	}

	private void registerBuiltInProperties(Model model) {
		String projectVersion = model.getVersion() != null
				? model.getVersion()
				: (model.getParent() != null ? model.getParent().getVersion() : null);
		if (projectVersion != null) {
			properties.put("project.version", projectVersion);
			properties.put("pom.version", projectVersion);
		}
		String groupId = model.getGroupId() != null
				? model.getGroupId()
				: (model.getParent() != null ? model.getParent().getGroupId() : null);
		if (groupId != null) {
			properties.put("project.groupId", groupId);
			properties.put("pom.groupId", groupId);
		}
	}

	private Path localParentPom(Model model, Path pomDir) {
		if (pomDir == null) {
			return null;
		}
		String relativePath = model.getParent().getRelativePath();
		if (relativePath == null) {
			relativePath = "../pom.xml"; // Maven's default
		}
		if (relativePath.isBlank()) {
			return null; // explicit empty <relativePath/> means "resolve from repository"
		}
		Path candidate = pomDir.resolve(relativePath).normalize();
		return Files.isDirectory(candidate) ? candidate.resolve("pom.xml") : candidate;
	}

	private String substitute(String value, int depth) {
		if (value == null || depth > MAX_DEPTH) {
			return value;
		}
		Matcher matcher = PLACEHOLDER.matcher(value);
		StringBuilder out = new StringBuilder();
		boolean replacedAny = false;
		while (matcher.find()) {
			String replacement = properties.get(matcher.group(1));
			if (replacement != null) {
				replacedAny = true;
				matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
			}
			else {
				matcher.appendReplacement(out, Matcher.quoteReplacement(matcher.group(0)));
			}
		}
		matcher.appendTail(out);
		String result = out.toString();
		if (replacedAny && PLACEHOLDER.matcher(result).find()) {
			return substitute(result, depth + 1);
		}
		return result;
	}

	private static String key(String groupId, String artifactId) {
		return groupId + ":" + artifactId;
	}
}
