package com.viandas.api.menu.persistence;

import java.util.UUID;

import com.viandas.api.menu.domain.*;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuItemRepository extends JpaRepository<MenuItem, UUID> {
	List<MenuItem> findByMenuIdOrderByCategoryAscIdAsc(UUID menuId);

	List<MenuItem> findByMenuIdAndAvailableCompaniesIdOrderByCategoryAscIdAsc(UUID menuId, UUID companyId);

	List<MenuItem> findByProductIdAndMenuStatus(UUID productId, MenuStatus menuStatus);
}
