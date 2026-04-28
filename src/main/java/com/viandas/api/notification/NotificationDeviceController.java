package com.viandas.api.notification;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viandas.api.auth.SecurityUtils;

@RestController
@RequestMapping("/me/notification-devices")
public class NotificationDeviceController {
	private final NotificationService notificationService;

	public NotificationDeviceController(NotificationService notificationService) {
		this.notificationService = notificationService;
	}

	@PostMapping
	NotificationService.DeviceResponse register(@RequestBody NotificationService.RegisterDeviceRequest request) {
		return notificationService.registerDevice(SecurityUtils.currentUser().userId(), request);
	}
}
