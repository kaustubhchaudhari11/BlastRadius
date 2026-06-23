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
@Table(name = "advisory")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Advisory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String source;

	@Column(name = "external_id", nullable = false, unique = true)
	private String externalId;

	@Column(nullable = false)
	private String ecosystem;

	@Column(name = "pkg_name", nullable = false)
	private String pkgName;

	@Column(name = "affected_versions", columnDefinition = "TEXT")
	private String affectedVersions;

	@Column(columnDefinition = "TEXT")
	private String summary;

	private String severity;

	@Column(name = "published_at")
	private Instant publishedAt;

	@OneToMany(mappedBy = "advisory", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private List<Finding> findings = new ArrayList<>();

}
