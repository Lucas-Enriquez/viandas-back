package com.viandas.api.notification.web;

import com.viandas.api.notification.application.*;
import com.viandas.api.notification.dto.request.RegisterDeviceRequest;
import com.viandas.api.notification.dto.response.DeviceResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viandas.api.auth.security.SecurityUtils;

@RestController
@RequestMapping("/me/notification-devices")
public class NotificationDeviceController {
	private final NotificationService notificationService;

	public NotificationDeviceController(NotificationService notificationService) {
		this.notificationService = notificationService;
	}

	@PostMapping
	DeviceResponse register(@Valid @RequestBody RegisterDeviceRequest request) {
		return notificationService.registerDevice(SecurityUtils.currentUser().userId(), request);
	}
}
