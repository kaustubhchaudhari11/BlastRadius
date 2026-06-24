package com.blastradius.ingestion;

import com.blastradius.model.Dependency;
import com.blastradius.model.Project;
import com.blastradius.model.ProjectRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registers a project and ingests its dependencies through the ecosystem adapters.
 *
 * <p>Flow: validate path → create Project → for each matching adapter, parse deps →
 * map language-neutral records to Dependency entities → persist (cascade).
 */
@Service
public class IngestionService {

	private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

	private final AdapterRegistry adapterRegistry;
	private final ProjectRepository projectRepository;

	public IngestionService(AdapterRegistry adapterRegistry, ProjectRepository projectRepository) {
		this.adapterRegistry = adapterRegistry;
		this.projectRepository = projectRepository;
	}

	@Transactional
	public Project ingestProject(String name, String sourcePath, String repoUrl) {
		Path repoRoot = Path.of(sourcePath);
		if (!Files.isDirectory(repoRoot)) {
			throw new IngestionException("Source path is not a directory: " + sourcePath);
		}

		List<EcosystemAdapter> matched = adapterRegistry.detectAll(repoRoot);
		if (matched.isEmpty()) {
			throw new IngestionException("No supported ecosystem detected at " + sourcePath);
		}

		Project project = Project.builder()
				.name(name)
				.sourcePath(sourcePath)
				.repoUrl(repoUrl)
				.createdAt(Instant.now())
				.build();

		for (EcosystemAdapter adapter : matched) {
			List<ParsedDependency> parsed = adapter.parseDependencies(repoRoot);
			for (ParsedDependency p : parsed) {
				Dependency dependency = Dependency.builder()
						.project(project)
						.ecosystem(p.ecosystem())
						.groupOrPkg(p.groupOrPkg())
						.artifactOrName(p.artifactOrName())
						.currentVersion(p.version())
						.build();
				project.getDependencies().add(dependency);
			}
			log.info("Adapter '{}' contributed {} dependencies", adapter.ecosystemId(), parsed.size());
		}

		return projectRepository.save(project);
	}
}
