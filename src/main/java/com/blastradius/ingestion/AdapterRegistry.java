package com.blastradius.ingestion;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Holds every registered {@link EcosystemAdapter} and selects the right one(s) for a repo.
 *
 * <p>Spring injects all adapter beans, so registering a new ecosystem (Phase 8) is just
 * adding a {@code @Component} — no edits here.
 */
@Component
public class AdapterRegistry {

	private final List<EcosystemAdapter> adapters;

	public AdapterRegistry(List<EcosystemAdapter> adapters) {
		this.adapters = adapters;
	}

	/** All adapters that recognise the repository (a repo may match more than one). */
	public List<EcosystemAdapter> detectAll(Path repoRoot) {
		return adapters.stream().filter(a -> a.detect(repoRoot)).toList();
	}

	/** Look up an adapter by its stable ecosystem id, e.g. "maven". */
	public Optional<EcosystemAdapter> byEcosystemId(String ecosystemId) {
		return adapters.stream().filter(a -> a.ecosystemId().equals(ecosystemId)).findFirst();
	}

	public List<EcosystemAdapter> all() {
		return adapters;
	}
}
