package com.blastradius.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "project")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Project {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String name;

	@Column(name = "source_path", nullable = false)
	private String sourcePath;

	@Column(name = "repo_url")
	private String repoUrl;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private List<Dependency> dependencies = new ArrayList<>();

	@OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private List<Finding> findings = new ArrayList<>();

}
