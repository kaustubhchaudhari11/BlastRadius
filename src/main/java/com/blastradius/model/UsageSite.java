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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "usage_site")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsageSite {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "dependency_id", nullable = false)
	private Dependency dependency;

	@Column(name = "file_path", nullable = false)
	private String filePath;

	@Column(name = "line_number", nullable = false)
	private Integer lineNumber;

	@Column(nullable = false)
	private String symbol;

	@Column(columnDefinition = "TEXT")
	private String snippet;

}
