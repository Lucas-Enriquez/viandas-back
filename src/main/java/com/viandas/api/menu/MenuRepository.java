package com.viandas.api.menu;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuRepository extends JpaRepository<Menu, Long> {
	Optional<Menu> findByIdAndCompanyCookId(Long id, Long cookId);

	Optional<Menu> findByCompanyIdAndMenuDate(Long companyId, LocalDate menuDate);

	@EntityGraph(attributePaths = {"items", "company"})
	Optional<Menu> findWithItemsById(Long id);
}
