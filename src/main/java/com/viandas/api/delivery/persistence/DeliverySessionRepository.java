package com.viandas.api.delivery.persistence;

import java.util.UUID;

import com.viandas.api.delivery.domain.*;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliverySessionRepository extends JpaRepository<DeliverySession, UUID> {
	Optional<DeliverySession> findByIdAndCompanyCookId(UUID id, UUID cookId);

	Optional<DeliverySession> findFirstByMenuIdAndCompanyIdAndStatusOrderByStartedAtDesc(
			UUID menuId,
			UUID companyId,
			DeliverySessionStatus status);
}
