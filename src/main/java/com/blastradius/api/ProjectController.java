package com.blastradius.api;

import com.blastradius.api.dto.CreateProjectRequest;
import com.blastradius.api.dto.ProjectResponse;
import com.blastradius.ingestion.IngestionException;
import com.blastradius.ingestion.IngestionService;
import com.blastradius.model.Project;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

	private final IngestionService ingestionService;

	public ProjectController(IngestionService ingestionService) {
		this.ingestionService = ingestionService;
	}

	@PostMapping
	public ResponseEntity<ProjectResponse> createProject(@RequestBody CreateProjectRequest request) {
		Project project = ingestionService.ingestProject(
				request.name(), request.sourcePath(), request.repoUrl());
		return ResponseEntity.status(HttpStatus.CREATED).body(ProjectResponse.from(project));
	}

	@ExceptionHandler(IngestionException.class)
	public ResponseEntity<String> handleIngestionError(IngestionException e) {
		return ResponseEntity.badRequest().body(e.getMessage());
	}
}
