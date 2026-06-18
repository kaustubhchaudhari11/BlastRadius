package com.blastradius.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "finding")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Finding {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "project_id", nullable = false)
	private Project project;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "dependency_id", nullable = false)
	private Dependency dependency;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "advisory_id", nullable = false)
	private Advisory advisory;

	@Column(name = "relevance_score")
	private Double relevanceScore;

	@Column(name = "triage_status")
	private String triageStatus;

	@Column(name = "migration_suggestion", columnDefinition = "TEXT")
	private String migrationSuggestion;

	@Column(name = "eval_passed")
	private Boolean evalPassed;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

}
