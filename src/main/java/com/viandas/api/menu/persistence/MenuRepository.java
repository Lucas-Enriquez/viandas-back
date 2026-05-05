package com.viandas.api.menu.persistence;

import java.util.UUID;

import com.viandas.api.menu.domain.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MenuRepository extends JpaRepository<Menu, UUID> {
	@EntityGraph(attributePaths = {"items", "company", "assignedCompanies", "items.availableCompanies"})
	Optional<Menu> findByIdAndCookId(UUID id, UUID cookId);

	Optional<Menu> findByCompanyIdAndMenuDate(UUID companyId, LocalDate menuDate);

	Optional<Menu> findByCookIdAndScopeAndMenuDate(UUID cookId, MenuScope scope, LocalDate menuDate);

	@EntityGraph(attributePaths = {"items", "company", "assignedCompanies", "items.availableCompanies"})
	List<Menu> findByCookIdOrderByMenuDateDescIdDesc(UUID cookId);

	@EntityGraph(attributePaths = {"items", "company", "assignedCompanies", "items.availableCompanies"})
	List<Menu> findByCookIdAndMenuDateOrderByIdDesc(UUID cookId, LocalDate menuDate);

	@EntityGraph(attributePaths = {"items", "company", "assignedCompanies", "items.availableCompanies"})
	@Query("""
			select distinct m
			from Menu m
			left join m.assignedCompanies ac
			where m.cook.id = :cookId
			and (m.company.id = :companyId or ac.id = :companyId)
			order by m.menuDate desc, m.id desc
			""")
	List<Menu> findVisibleByCookAndCompany(@Param("cookId") UUID cookId, @Param("companyId") UUID companyId);

	@EntityGraph(attributePaths = {"items", "company", "assignedCompanies", "items.availableCompanies"})
	@Query("""
			select distinct m
			from Menu m
			left join m.assignedCompanies ac
			where m.cook.id = :cookId
			and m.menuDate = :menuDate
			and (m.company.id = :companyId or ac.id = :companyId)
			order by m.id desc
			""")
	List<Menu> findVisibleByCookAndCompanyAndDate(
			@Param("cookId") UUID cookId,
			@Param("companyId") UUID companyId,
			@Param("menuDate") LocalDate menuDate);

	@EntityGraph(attributePaths = {"items", "company", "assignedCompanies", "items.availableCompanies"})
	Optional<Menu> findWithItemsById(UUID id);
}
