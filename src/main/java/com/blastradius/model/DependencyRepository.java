package com.blastradius.model;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DependencyRepository extends JpaRepository<Dependency, Long> {

	List<Dependency> findByProjectId(Long projectId);

	List<Dependency> findByProjectIdAndEcosystem(Long projectId, String ecosystem);

}
