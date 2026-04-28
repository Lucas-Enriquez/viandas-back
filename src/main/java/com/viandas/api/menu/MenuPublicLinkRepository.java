package com.viandas.api.menu;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuPublicLinkRepository extends JpaRepository<MenuPublicLink, Long> {
	Optional<MenuPublicLink> findByTokenHashAndActiveTrue(String tokenHash);

	List<MenuPublicLink> findByMenuIdAndActiveTrue(Long menuId);
}
