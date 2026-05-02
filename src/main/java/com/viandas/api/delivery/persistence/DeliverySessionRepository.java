package com.viandas.api.delivery.persistence;

import com.viandas.api.delivery.domain.*;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliverySessionRepository extends JpaRepository<DeliverySession, Long> {
	Optional<DeliverySession> findByIdAndCompanyCookId(Long id, Long cookId);

	Optional<DeliverySession> findFirstByMenuIdAndCompanyIdAndStatusOrderByStartedAtDesc(
			Long menuId,
			Long companyId,
			DeliverySessionStatus status);
}
