package com.viandas.api.notification.application;

import com.viandas.api.notification.domain.*;
import com.viandas.api.notification.dto.request.RegisterDeviceRequest;
import com.viandas.api.notification.dto.response.DeviceResponse;
import com.viandas.api.notification.persistence.*;
import java.time.Instant;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.viandas.api.user.domain.User;
import com.viandas.api.user.persistence.UserRepository;

@Service
public class NotificationService {
	private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

	private final NotificationDeviceRepository notificationDeviceRepository;
	private final UserRepository userRepository;

	public NotificationService(NotificationDeviceRepository notificationDeviceRepository, UserRepository userRepository) {
		this.notificationDeviceRepository = notificationDeviceRepository;
		this.userRepository = userRepository;
	}

	public DeviceResponse registerDevice(Long userId, RegisterDeviceRequest request) {
		User user = userRepository.findById(userId).orElseThrow();
		NotificationDevice device = notificationDeviceRepository.findByToken(request.token()).orElseGet(NotificationDevice::new);
		device.setUser(user);
		device.setToken(request.token());
		device.setPlatform(request.platform());
		device.setLastSeenAt(Instant.now());
		NotificationDevice saved = notificationDeviceRepository.save(device);
		return new DeviceResponse(saved.getId(), saved.getPlatform(), saved.getLastSeenAt());
	}

	public void notifyUser(Long userId, String title, String body, Map<String, String> data) {
		var devices = notificationDeviceRepository.findByUserId(userId);
		if (devices.isEmpty()) {
			return;
		}
		log.info("FCM noop: would notify user {} with title '{}' and {} device(s)", userId, title, devices.size());
	}

}
