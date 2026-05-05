package com.viandas.api.company.persistence;

import java.util.UUID;

import com.viandas.api.company.domain.*;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, UUID> {
	List<Company> findByCookIdOrderByName(UUID cookId);

	List<Company> findByIdInAndCookId(List<UUID> ids, UUID cookId);

	Optional<Company> findByIdAndCookId(UUID id, UUID cookId);

	Optional<Company> findBySlug(String slug);

	boolean existsBySlug(String slug);
}
