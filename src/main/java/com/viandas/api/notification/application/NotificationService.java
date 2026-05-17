package com.viandas.api.notification.application;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;

import com.viandas.api.notification.domain.*;
import com.viandas.api.notification.dto.request.RegisterDeviceRequest;
import com.viandas.api.notification.dto.response.DeviceResponse;
import com.viandas.api.notification.persistence.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.viandas.api.user.domain.User;
import com.viandas.api.user.persistence.UserRepository;

@Service
public class NotificationService {
	private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

	private final NotificationDeviceRepository notificationDeviceRepository;
	private final UserRepository userRepository;
	private final Optional<FirebaseMessaging> firebaseMessaging;

	public NotificationService(
			NotificationDeviceRepository notificationDeviceRepository,
			UserRepository userRepository,
			Optional<FirebaseMessaging> firebaseMessaging) {
		this.notificationDeviceRepository = notificationDeviceRepository;
		this.userRepository = userRepository;
		this.firebaseMessaging = firebaseMessaging;
	}

	public DeviceResponse registerDevice(UUID userId, RegisterDeviceRequest request) {
		User user = userRepository.findById(userId).orElseThrow();
		NotificationDevice device = notificationDeviceRepository.findByToken(request.token()).orElseGet(NotificationDevice::new);
		device.setUser(user);
		device.setToken(request.token());
		device.setPlatform(request.platform());
		device.setLastSeenAt(Instant.now());
		NotificationDevice saved = notificationDeviceRepository.save(device);
		return new DeviceResponse(saved.getId(), saved.getPlatform(), saved.getLastSeenAt());
	}

	@Async
	public void notifyUser(UUID userId, String title, String body, Map<String, String> data) {
		List<NotificationDevice> devices = notificationDeviceRepository.findByUserId(userId);
		if (devices.isEmpty()) {
			return;
		}
		if (firebaseMessaging.isEmpty()) {
			log.info("FCM noop (no firebase bean): user {} title '{}' devices {}", userId, title, devices.size());
			return;
		}
		List<String> tokens = devices.stream().map(NotificationDevice::getToken).toList();
		MulticastMessage message = MulticastMessage.builder()
				.addAllTokens(tokens)
				.setNotification(Notification.builder().setTitle(title).setBody(body).build())
				.putAllData(data == null ? Map.of() : data)
				.build();
		try {
			BatchResponse response = firebaseMessaging.get().sendEachForMulticast(message);
			for (int i = 0; i < response.getResponses().size(); i++) {
				SendResponse sendResponse = response.getResponses().get(i);
				if (!sendResponse.isSuccessful()) {
					handleFailedSend(tokens.get(i), sendResponse.getException());
				}
			}
			log.info("FCM sent to user {}: success={}, failure={}", userId, response.getSuccessCount(), response.getFailureCount());
		} catch (FirebaseMessagingException exception) {
			log.error("FCM send failed for user {}", userId, exception);
		}
	}

	private void handleFailedSend(String token, FirebaseMessagingException exception) {
		MessagingErrorCode code = exception == null ? null : exception.getMessagingErrorCode();
		if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
			log.info("Removing invalid FCM token: {}", maskToken(token));
			try {
				notificationDeviceRepository.deleteByToken(token);
			} catch (Exception deleteException) {
				log.warn("Failed to delete invalid FCM token", deleteException);
			}
		} else {
			log.warn("FCM send failed for token {}: code={} message={}", maskToken(token), code, exception != null ? exception.getMessage() : "n/a");
		}
	}

	private static String maskToken(String token) {
		if (token == null || token.length() < 10) return "***";
		return token.substring(0, 6) + "..." + token.substring(token.length() - 4);
	}

}
