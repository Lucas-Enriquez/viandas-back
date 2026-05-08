package com.viandas.api.menu.persistence;

import java.util.UUID;

import com.viandas.api.menu.domain.*;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MenuItemRepository extends JpaRepository<MenuItem, UUID> {
	List<MenuItem> findByMenuIdOrderByCategoryAscIdAsc(UUID menuId);

	List<MenuItem> findByMenuIdAndAvailableCompaniesIdOrderByCategoryAscIdAsc(UUID menuId, UUID companyId);

    /** Removes the company from the menu_item_companies join table (GLOBAL menus). */
    @Modifying
    @Query(value = "DELETE FROM menu_item_companies WHERE company_id = :companyId", nativeQuery = true)
    void removeCompanyFromAllMenuItems(@Param("companyId") UUID companyId);

    /** Deletes all items belonging to menus owned by the company (COMPANY-scope menus). */
    @Modifying
    @Query("DELETE FROM MenuItem i WHERE i.menu.company.id = :companyId")
    void deleteByMenuCompanyId(@Param("companyId") UUID companyId);
}
