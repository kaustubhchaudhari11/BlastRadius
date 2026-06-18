package com.blastradius.model;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FindingRepository extends JpaRepository<Finding, Long> {

	List<Finding> findByProjectId(Long projectId);

}
