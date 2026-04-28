package com.viandas.api.company;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {
	List<Company> findByCookIdOrderByName(Long cookId);

	Optional<Company> findByIdAndCookId(Long id, Long cookId);

	Optional<Company> findBySlug(String slug);

	boolean existsBySlug(String slug);
}
