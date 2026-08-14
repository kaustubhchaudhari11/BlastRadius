package com.blastradius.ingestion;

import java.util.Optional;
import org.apache.maven.model.Model;

/**
 * Loads a POM that is not present on disk — a remote parent or an imported BOM.
 *
 * <p>Exists as a seam so version resolution can be unit-tested offline and so an air-gapped
 * deployment can supply a no-op or mirror-backed implementation.
 */
public interface PomFetcher {

	/**
	 * @return the POM model, or empty when it cannot be retrieved (offline, 404, malformed).
	 *         Callers must degrade gracefully rather than fail ingestion.
	 */
	Optional<Model> fetch(String groupId, String artifactId, String version);
}
