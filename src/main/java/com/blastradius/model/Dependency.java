package com.blastradius.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "dependency")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dependency {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "project_id", nullable = false)
	private Project project;

	@Column(nullable = false)
	private String ecosystem;

	@Column(name = "group_or_pkg", nullable = false)
	private String groupOrPkg;

	@Column(name = "artifact_or_name", nullable = false)
	private String artifactOrName;

	@Column(name = "current_version", nullable = false)
	private String currentVersion;

	@Column(name = "latest_version")
	private String latestVersion;

	@OneToMany(mappedBy = "dependency", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private List<UsageSite> usageSites = new ArrayList<>();

	@OneToMany(mappedBy = "dependency", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private List<Finding> findings = new ArrayList<>();

}
