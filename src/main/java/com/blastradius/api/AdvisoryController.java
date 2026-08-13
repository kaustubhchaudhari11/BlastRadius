package com.blastradius.api;

import com.blastradius.advisory.AdvisoryException;
import com.blastradius.advisory.AdvisoryRefreshResult;
import com.blastradius.advisory.AdvisoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/advisories")
public class AdvisoryController {

	private final AdvisoryService advisoryService;

	public AdvisoryController(AdvisoryService advisoryService) {
		this.advisoryService = advisoryService;
	}

	@PostMapping("/refresh")
	public ResponseEntity<AdvisoryRefreshResult> refresh(@PathVariable Long projectId) {
		return ResponseEntity.ok(advisoryService.refreshForProject(projectId));
	}

	@ExceptionHandler(AdvisoryException.class)
	public ResponseEntity<String> handleAdvisoryError(AdvisoryException e) {
		return ResponseEntity.badRequest().body(e.getMessage());
	}
}
