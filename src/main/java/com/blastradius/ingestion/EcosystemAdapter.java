package com.blastradius.ingestion;

import com.blastradius.model.Dependency;
import com.blastradius.model.UsageSite;
import java.nio.file.Path;
import java.util.List;

/**
 * The single extensibility seam of Blast Radius.
 *
 * <p>All language-specific logic lives behind this interface. Advisory ingestion,
 * the agent pipeline, the REST API, and the UI are ecosystem-agnostic and never
 * implement against a concrete language. Adding a new ecosystem (e.g. Python) means
 * implementing one of these — nothing downstream changes.
 */
public interface EcosystemAdapter {

	/** Stable identifier stored on every Dependency/Advisory row, e.g. "maven" or "pypi". */
	String ecosystemId();

	/** Whether this adapter recognises the given repository (e.g. a pom.xml is present). */
	boolean detect(Path repoRoot);

	/** Parse the manifest(s) into language-neutral dependency records. */
	List<ParsedDependency> parseDependencies(Path repoRoot);

	/**
	 * Find the source locations where the project actually uses a dependency.
	 * Implemented per-language in Phase 4; the Maven implementation is a stub for now.
	 */
	List<UsageSite> scanUsage(Path repoRoot, Dependency dependency);
}
