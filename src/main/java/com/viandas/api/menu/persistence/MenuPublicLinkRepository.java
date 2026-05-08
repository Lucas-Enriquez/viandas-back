package com.viandas.api.menu.persistence;

import java.util.UUID;

import com.viandas.api.menu.domain.*;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MenuPublicLinkRepository extends JpaRepository<MenuPublicLink, UUID> {
	Optional<MenuPublicLink> findByTokenHashAndActiveTrue(String tokenHash);

	List<MenuPublicLink> findByMenuIdAndActiveTrue(UUID menuId);

    /** Deletes links tied directly to the company and links for menus owned by the company. */
    @Modifying
    @Query("DELETE FROM MenuPublicLink l WHERE l.company.id = :companyId OR l.menu.company.id = :companyId")
    void deleteByCompanyIdOrMenuCompanyId(@Param("companyId") UUID companyId);
}
