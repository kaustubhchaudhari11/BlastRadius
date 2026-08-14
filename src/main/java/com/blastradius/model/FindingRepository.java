package com.blastradius.model;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FindingRepository extends JpaRepository<Finding, Long> {

	List<Finding> findByProjectId(Long projectId);

	/** Backs the upsert in advisory refresh; the pair is unique (see V2 migration). */
	Optional<Finding> findByDependencyIdAndAdvisoryId(Long dependencyId, Long advisoryId);

	List<Finding> findByProjectIdAndTriageStatus(Long projectId, String triageStatus);

}
