package com.blastradius.model;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdvisoryRepository extends JpaRepository<Advisory, Long> {

	Optional<Advisory> findByExternalId(String externalId);

	List<Advisory> findByEcosystemAndPkgName(String ecosystem, String pkgName);

}
