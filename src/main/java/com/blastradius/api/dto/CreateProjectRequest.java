package com.blastradius.api.dto;

/** Request body for registering a project to analyse. */
public record CreateProjectRequest(String name, String sourcePath, String repoUrl) {
}
