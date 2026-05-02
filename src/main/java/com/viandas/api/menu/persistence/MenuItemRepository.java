package com.viandas.api.menu.persistence;

import com.viandas.api.menu.domain.*;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
	List<MenuItem> findByMenuIdOrderByCategoryAscIdAsc(Long menuId);
}
