package com.viandas.api.menu.persistence;

import com.viandas.api.menu.domain.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuRepository extends JpaRepository<Menu, Long> {
	Optional<Menu> findByIdAndCompanyCookId(Long id, Long cookId);

	Optional<Menu> findByCompanyIdAndMenuDate(Long companyId, LocalDate menuDate);

	@EntityGraph(attributePaths = {"items", "company"})
	List<Menu> findByCompanyCookIdOrderByMenuDateDescIdDesc(Long cookId);

	@EntityGraph(attributePaths = {"items", "company"})
	List<Menu> findByCompanyCookIdAndCompanyIdOrderByMenuDateDescIdDesc(Long cookId, Long companyId);

	@EntityGraph(attributePaths = {"items", "company"})
	List<Menu> findByCompanyCookIdAndMenuDateOrderByIdDesc(Long cookId, LocalDate menuDate);

	@EntityGraph(attributePaths = {"items", "company"})
	List<Menu> findByCompanyCookIdAndCompanyIdAndMenuDateOrderByIdDesc(Long cookId, Long companyId, LocalDate menuDate);

	@EntityGraph(attributePaths = {"items", "company"})
	Optional<Menu> findWithItemsById(Long id);
}
