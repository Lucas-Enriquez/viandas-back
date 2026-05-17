package com.viandas.api.notification.persistence;

import java.util.UUID;

import com.viandas.api.notification.domain.*;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

public interface NotificationDeviceRepository extends JpaRepository<NotificationDevice, UUID> {
	Optional<NotificationDevice> findByToken(String token);

	List<NotificationDevice> findByUserId(UUID userId);

	@Modifying
	@Transactional
	void deleteByToken(String token);
}
