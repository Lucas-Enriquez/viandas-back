package com.viandas.api.menu.persistence;

import java.util.UUID;

import com.viandas.api.menu.domain.*;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuPublicLinkRepository extends JpaRepository<MenuPublicLink, UUID> {
	Optional<MenuPublicLink> findByTokenHashAndActiveTrue(String tokenHash);

	List<MenuPublicLink> findByMenuIdAndActiveTrue(UUID menuId);
}
