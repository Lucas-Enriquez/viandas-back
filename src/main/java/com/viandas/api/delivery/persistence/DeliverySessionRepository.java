package com.viandas.api.delivery.persistence;

import java.util.UUID;

import com.viandas.api.delivery.domain.*;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeliverySessionRepository extends JpaRepository<DeliverySession, UUID> {
	Optional<DeliverySession> findByIdAndCompanyCookId(UUID id, UUID cookId);

	Optional<DeliverySession> findFirstByMenuIdAndCompanyIdAndStatusOrderByStartedAtDesc(
			UUID menuId,
			UUID companyId,
			DeliverySessionStatus status);

    @Modifying
    @Query("DELETE FROM DeliverySession d WHERE d.company.id = :companyId")
    void deleteByCompanyId(@Param("companyId") UUID companyId);
}
