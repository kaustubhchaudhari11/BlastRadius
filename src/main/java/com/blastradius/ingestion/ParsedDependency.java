package com.blastradius.ingestion;

/**
 * Language-neutral representation of a single dependency parsed from a manifest.
 *
 * <p>Adapters return these instead of JPA entities so that parsing stays free of
 * persistence concerns (no project_id, no transactions). The IngestionService is
 * responsible for converting these into persisted {@code Dependency} entities.
 */
public record ParsedDependency(
		String ecosystem,
		String groupOrPkg,
		String artifactOrName,
		String version) {
}
