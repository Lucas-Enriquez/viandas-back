package com.viandas.api.notification;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationDeviceRepository extends JpaRepository<NotificationDevice, Long> {
	Optional<NotificationDevice> findByToken(String token);

	List<NotificationDevice> findByUserId(Long userId);
}
