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
 * express its version three indirect ways, all handled here:
 * <ul>
 *   <li>a {@code ${property}} placeholder defined in {@code <properties>} (this pom or a parent);</li>
 *   <li>no version at all, with the value supplied by {@code <dependencyManagement>};</li>
 *   <li>built-in expressions such as {@code ${project.version}}.</li>
 * </ul>
 *
 * <p>Scope limit (documented MVP boundary): parent POMs are followed only when their
 * {@code <relativePath>} resolves to a file on disk. Versions that live exclusively in a
 * <strong>remote</strong> BOM (e.g. {@code spring-boot-starter-parent}) are not downloaded,
 * so such dependencies remain unresolved and are reported as {@code "unspecified"} for the
 * consumer to handle.
 */
class MavenVersionResolver {

	private static final Logger log = LoggerFactory.getLogger(MavenVersionResolver.class);
	private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}]+)}");
	private static final int MAX_PARENT_DEPTH = 10;

	private final Map<String, String> properties = new HashMap<>();
	private final Map<String, String> managedVersions = new HashMap<>();

	MavenVersionResolver(Model rootModel, Path rootPomDir) {
		accumulate(rootModel, rootPomDir, 0);
	}

	/**
	 * @return the fully concrete version, or empty if it cannot be resolved on disk.
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

	/** Walk parents first so that a child's properties/management override inherited ones. */
	private void accumulate(Model model, Path pomDir, int depth) {
		if (model.getParent() != null && depth < MAX_PARENT_DEPTH) {
			Path parentPom = localParentPom(model, pomDir);
			if (parentPom != null && Files.isRegularFile(parentPom)) {
				try (InputStream in = Files.newInputStream(parentPom)) {
					Model parent = new MavenXpp3Reader().read(in);
					accumulate(parent, parentPom.getParent(), depth + 1);
				}
				catch (Exception e) {
					log.debug("Could not read parent pom at {}: {}", parentPom, e.getMessage());
				}
			}
		}

		model.getProperties().forEach((k, v) -> properties.put(String.valueOf(k), String.valueOf(v)));

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

		if (model.getDependencyManagement() != null) {
			for (org.apache.maven.model.Dependency d : model.getDependencyManagement().getDependencies()) {
				if (d.getVersion() != null && !d.getVersion().isBlank()) {
					managedVersions.putIfAbsent(key(d.getGroupId(), d.getArtifactId()), d.getVersion());
				}
			}
		}
	}

	private Path localParentPom(Model model, Path pomDir) {
		String relativePath = model.getParent().getRelativePath();
		if (relativePath == null) {
			relativePath = "../pom.xml"; // Maven's default
		}
		if (relativePath.isBlank()) {
			return null; // explicit empty <relativePath/> means "resolve from repository", not disk
		}
		Path candidate = pomDir.resolve(relativePath).normalize();
		return Files.isDirectory(candidate) ? candidate.resolve("pom.xml") : candidate;
	}

	private String substitute(String value, int depth) {
		if (depth > MAX_PARENT_DEPTH) {
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
