package com.viandas.api.delivery;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.viandas.api.auth.CurrentUser;
import com.viandas.api.menu.Menu;
import com.viandas.api.menu.MenuService;
import com.viandas.api.notification.NotificationService;
import com.viandas.api.notification.OrderEventBroadcaster;
import com.viandas.api.order.CustomerOrder;
import com.viandas.api.order.OrderRepository;
import com.viandas.api.order.OrderStatus;
import com.viandas.api.shared.ApiException;
import com.viandas.api.user.User;
import com.viandas.api.user.UserRepository;

@Service
public class DeliveryService {
	private final DeliverySessionRepository deliverySessionRepository;
	private final DeliveryLocationUpdateRepository deliveryLocationUpdateRepository;
	private final MenuService menuService;
	private final OrderRepository orderRepository;
	private final UserRepository userRepository;
	private final NotificationService notificationService;
	private final OrderEventBroadcaster orderEventBroadcaster;
	private final long nearbyThresholdMeters;
	private final long sessionTtlMinutes;

	public DeliveryService(
			DeliverySessionRepository deliverySessionRepository,
			DeliveryLocationUpdateRepository deliveryLocationUpdateRepository,
			MenuService menuService,
			OrderRepository orderRepository,
			UserRepository userRepository,
			NotificationService notificationService,
			OrderEventBroadcaster orderEventBroadcaster,
			@Value("${viandas.delivery.nearby-threshold-meters}") long nearbyThresholdMeters,
			@Value("${viandas.delivery.session-ttl-minutes}") long sessionTtlMinutes) {
		this.deliverySessionRepository = deliverySessionRepository;
		this.deliveryLocationUpdateRepository = deliveryLocationUpdateRepository;
		this.menuService = menuService;
		this.orderRepository = orderRepository;
		this.userRepository = userRepository;
		this.notificationService = notificationService;
		this.orderEventBroadcaster = orderEventBroadcaster;
		this.nearbyThresholdMeters = nearbyThresholdMeters;
		this.sessionTtlMinutes = sessionTtlMinutes;
	}

	@Transactional
	public DeliverySessionResponse start(CurrentUser currentUser, StartDeliverySessionRequest request) {
		Menu menu = menuService.requireOwnedMenu(currentUser, request.menuId());
		if (!menu.getCompany().getId().equals(request.companyId())) {
			throw ApiException.badRequest("Menu does not belong to company");
		}
		User cook = userRepository.findById(currentUser.userId()).orElseThrow(() -> ApiException.unauthorized("User not found"));
		Instant now = Instant.now();
		DeliverySession session = new DeliverySession();
		session.setCompany(menu.getCompany());
		session.setMenu(menu);
		session.setCook(cook);
		session.setStatus(DeliverySessionStatus.ACTIVE);
		session.setStartedAt(now);
		session.setExpiresAt(now.plusSeconds(sessionTtlMinutes * 60));
		DeliverySession saved = deliverySessionRepository.save(session);
		updateOpenOrders(menu, OrderStatus.OUT_FOR_DELIVERY);
		orderEventBroadcaster.publish(menu.getCompany().getId(), "delivery.started", toResponse(saved, DeliveryPublicSignal.OUT_FOR_DELIVERY));
		return toResponse(saved, DeliveryPublicSignal.OUT_FOR_DELIVERY);
	}

	@Transactional
	public DeliverySessionResponse updateLocation(CurrentUser currentUser, Long sessionId, LocationUpdateRequest request) {
		DeliverySession session = requireOwnedActiveSession(currentUser, sessionId);
		BigDecimal approxLat = roundCoordinate(request.latitude());
		BigDecimal approxLon = roundCoordinate(request.longitude());
		DeliveryPublicSignal signal = publicSignal(session, approxLat, approxLon);
		session.setLastApproxLatitude(approxLat);
		session.setLastApproxLongitude(approxLon);
		session.setLastLocationAt(Instant.now());

		DeliveryLocationUpdate update = new DeliveryLocationUpdate();
		update.setDeliverySession(session);
		update.setApproxLatitude(approxLat);
		update.setApproxLongitude(approxLon);
		update.setAccuracyMeters(request.accuracyMeters());
		update.setPublicSignal(signal);
		deliveryLocationUpdateRepository.save(update);

		if (signal == DeliveryPublicSignal.NEARBY) {
			updateOpenOrders(session.getMenu(), OrderStatus.NEARBY);
		}
		DeliverySessionResponse response = toResponse(session, signal);
		orderEventBroadcaster.publish(session.getCompany().getId(), "delivery.location", response);
		return response;
	}

	@Transactional
	public DeliverySessionResponse finish(CurrentUser currentUser, Long sessionId) {
		DeliverySession session = deliverySessionRepository.findByIdAndCompanyCookId(sessionId, currentUser.userId())
				.orElseThrow(() -> ApiException.notFound("Delivery session not found"));
		session.setStatus(DeliverySessionStatus.FINISHED);
		session.setFinishedAt(Instant.now());
		DeliverySessionResponse response = toResponse(session, DeliveryPublicSignal.DELIVERED);
		orderEventBroadcaster.publish(session.getCompany().getId(), "delivery.finished", response);
		return response;
	}

	private DeliverySession requireOwnedActiveSession(CurrentUser currentUser, Long sessionId) {
		DeliverySession session = deliverySessionRepository.findByIdAndCompanyCookId(sessionId, currentUser.userId())
				.orElseThrow(() -> ApiException.notFound("Delivery session not found"));
		if (session.getStatus() != DeliverySessionStatus.ACTIVE) {
			throw ApiException.conflict("Delivery session is not active");
		}
		if (session.getExpiresAt().isBefore(Instant.now())) {
			session.setStatus(DeliverySessionStatus.EXPIRED);
			throw ApiException.conflict("Delivery session expired");
		}
		return session;
	}

	private DeliveryPublicSignal publicSignal(DeliverySession session, BigDecimal approxLat, BigDecimal approxLon) {
		if (session.getCompany().getLatitude() == null || session.getCompany().getLongitude() == null) {
			return DeliveryPublicSignal.UNKNOWN;
		}
		double meters = distanceMeters(
				approxLat.doubleValue(),
				approxLon.doubleValue(),
				session.getCompany().getLatitude().doubleValue(),
				session.getCompany().getLongitude().doubleValue());
		return meters <= nearbyThresholdMeters ? DeliveryPublicSignal.NEARBY : DeliveryPublicSignal.OUT_FOR_DELIVERY;
	}

	private void updateOpenOrders(Menu menu, OrderStatus status) {
		List<OrderStatus> openStatuses = List.of(OrderStatus.RECEIVED, OrderStatus.PREPARING, OrderStatus.OUT_FOR_DELIVERY);
		List<CustomerOrder> orders = orderRepository.findByMenuIdAndCompanyIdAndStatusIn(menu.getId(), menu.getCompany().getId(), openStatuses);
		for (CustomerOrder order : orders) {
			order.setStatus(status);
			order.setUpdatedAt(Instant.now());
			if (order.getCustomer() != null) {
				notificationService.notifyUser(
						order.getCustomer().getId(),
						"Pedido actualizado",
						status == OrderStatus.NEARBY ? "Esta cerca." : "Tu pedido ya salio.",
						Map.of("orderId", order.getId().toString()));
			}
		}
	}

	private static BigDecimal roundCoordinate(BigDecimal coordinate) {
		if (coordinate == null) {
			throw ApiException.badRequest("Latitude and longitude are required");
		}
		return coordinate.setScale(3, RoundingMode.HALF_UP);
	}

	private static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
		double earthRadius = 6371000;
		double dLat = Math.toRadians(lat2 - lat1);
		double dLon = Math.toRadians(lon2 - lon1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
				+ Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
				* Math.sin(dLon / 2) * Math.sin(dLon / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		return earthRadius * c;
	}

	private DeliverySessionResponse toResponse(DeliverySession session, DeliveryPublicSignal signal) {
		return new DeliverySessionResponse(
				session.getId(),
				session.getCompany().getId(),
				session.getMenu().getId(),
				session.getStatus(),
				signal,
				session.getStartedAt(),
				session.getFinishedAt(),
				session.getExpiresAt(),
				session.getLastLocationAt());
	}

	public record StartDeliverySessionRequest(Long companyId, Long menuId) {
	}

	public record LocationUpdateRequest(BigDecimal latitude, BigDecimal longitude, BigDecimal accuracyMeters) {
	}

	public record DeliverySessionResponse(
			Long id,
			Long companyId,
			Long menuId,
			DeliverySessionStatus status,
			DeliveryPublicSignal publicSignal,
			Instant startedAt,
			Instant finishedAt,
			Instant expiresAt,
			Instant lastLocationAt) {
	}
}
