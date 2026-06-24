package com.blastradius.api.dto;

import com.blastradius.model.Project;
import java.util.List;

/** API view of a registered project plus its ingested dependencies. */
public record ProjectResponse(
		Long id,
		String name,
		String sourcePath,
		String repoUrl,
		List<DependencyResponse> dependencies) {

	public static ProjectResponse from(Project project) {
		List<DependencyResponse> deps = project.getDependencies().stream()
				.map(DependencyResponse::from)
				.toList();
		return new ProjectResponse(
				project.getId(),
				project.getName(),
				project.getSourcePath(),
				project.getRepoUrl(),
				deps);
	}
}
