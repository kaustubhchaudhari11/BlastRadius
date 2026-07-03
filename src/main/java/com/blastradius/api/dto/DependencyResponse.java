package com.blastradius.api.dto;

import com.blastradius.model.Dependency;

/** API view of a stored dependency. */
public record DependencyResponse(
		Long id,
		String ecosystem,
		String groupOrPkg,
		String artifactOrName,
		String currentVersion) {

	public static DependencyResponse from(Dependency d) {
		return new DependencyResponse(
				d.getId(),
				d.getEcosystem(),
				d.getGroupOrPkg(),
				d.getArtifactOrName(),
				d.getCurrentVersion());
	}
}
