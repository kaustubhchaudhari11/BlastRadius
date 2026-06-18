package com.blastradius.model;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsageSiteRepository extends JpaRepository<UsageSite, Long> {

	List<UsageSite> findByDependencyId(Long dependencyId);

}
