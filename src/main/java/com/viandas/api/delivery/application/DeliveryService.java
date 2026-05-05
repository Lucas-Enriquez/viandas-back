package com.viandas.api.delivery.application;

import java.util.UUID;

import com.viandas.api.delivery.domain.*;
import com.viandas.api.delivery.dto.request.LocationUpdateRequest;
import com.viandas.api.delivery.dto.request.StartDeliverySessionRequest;
import com.viandas.api.delivery.dto.response.DeliverySessionResponse;
import com.viandas.api.delivery.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.viandas.api.auth.security.CurrentUser;
import com.viandas.api.company.domain.Company;
import com.viandas.api.company.persistence.CompanyRepository;
import com.viandas.api.menu.domain.Menu;
import com.viandas.api.menu.application.MenuService;
import com.viandas.api.notification.application.NotificationService;
import com.viandas.api.notification.application.OrderEventBroadcaster;
import com.viandas.api.order.domain.CustomerOrder;
import com.viandas.api.order.persistence.OrderRepository;
import com.viandas.api.order.domain.OrderStatus;
import com.viandas.api.shared.ApiException;
import com.viandas.api.user.domain.User;
import com.viandas.api.user.persistence.UserRepository;

@Service
public class DeliveryService {
	private final DeliverySessionRepository deliverySessionRepository;
	private final DeliveryLocationUpdateRepository deliveryLocationUpdateRepository;
	private final MenuService menuService;
	private final CompanyRepository companyRepository;
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
			CompanyRepository companyRepository,
			OrderRepository orderRepository,
			UserRepository userRepository,
			NotificationService notificationService,
			OrderEventBroadcaster orderEventBroadcaster,
			@Value("${viandas.delivery.nearby-threshold-meters}") long nearbyThresholdMeters,
			@Value("${viandas.delivery.session-ttl-minutes}") long sessionTtlMinutes) {
		this.deliverySessionRepository = deliverySessionRepository;
		this.deliveryLocationUpdateRepository = deliveryLocationUpdateRepository;
		this.menuService = menuService;
		this.companyRepository = companyRepository;
		this.orderRepository = orderRepository;
		this.userRepository = userRepository;
		this.notificationService = notificationService;
		this.orderEventBroadcaster = orderEventBroadcaster;
		this.nearbyThresholdMeters = nearbyThresholdMeters;
		this.sessionTtlMinutes = sessionTtlMinutes;
	}

	@Transactional
	public DeliverySessionResponse start(CurrentUser currentUser, StartDeliverySessionRequest request) {
		Menu menu = menuService.requireOwnedMenuForCompany(currentUser, request.menuId(), request.companyId());
		Company company = companyRepository.findByIdAndCookId(request.companyId(), currentUser.userId())
				.orElseThrow(() -> ApiException.notFound("Company not found"));
		User cook = userRepository.findById(currentUser.userId()).orElseThrow(() -> ApiException.unauthorized("User not found"));
		Instant now = Instant.now();
		DeliverySession session = new DeliverySession();
		session.setCompany(company);
		session.setMenu(menu);
		session.setCook(cook);
		session.setStatus(DeliverySessionStatus.ACTIVE);
		session.setStartedAt(now);
		session.setExpiresAt(now.plusSeconds(sessionTtlMinutes * 60));
		DeliverySession saved = deliverySessionRepository.save(session);
		updateOpenOrders(menu, company, OrderStatus.OUT_FOR_DELIVERY);
		orderEventBroadcaster.publish(company.getId(), "delivery.started", toResponse(saved, DeliveryPublicSignal.OUT_FOR_DELIVERY));
		return toResponse(saved, DeliveryPublicSignal.OUT_FOR_DELIVERY);
	}

	@Transactional
	public DeliverySessionResponse updateLocation(CurrentUser currentUser, UUID sessionId, LocationUpdateRequest request) {
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
			updateOpenOrders(session.getMenu(), session.getCompany(), OrderStatus.NEARBY);
		}
		DeliverySessionResponse response = toResponse(session, signal);
		orderEventBroadcaster.publish(session.getCompany().getId(), "delivery.location", response);
		return response;
	}

	@Transactional
	public DeliverySessionResponse finish(CurrentUser currentUser, UUID sessionId) {
		DeliverySession session = deliverySessionRepository.findByIdAndCompanyCookId(sessionId, currentUser.userId())
				.orElseThrow(() -> ApiException.notFound("Delivery session not found"));
		session.setStatus(DeliverySessionStatus.FINISHED);
		session.setFinishedAt(Instant.now());
		DeliverySessionResponse response = toResponse(session, DeliveryPublicSignal.DELIVERED);
		orderEventBroadcaster.publish(session.getCompany().getId(), "delivery.finished", response);
		return response;
	}

	private DeliverySession requireOwnedActiveSession(CurrentUser currentUser, UUID sessionId) {
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

	private void updateOpenOrders(Menu menu, Company company, OrderStatus status) {
		List<OrderStatus> openStatuses = List.of(OrderStatus.RECEIVED, OrderStatus.PREPARING, OrderStatus.OUT_FOR_DELIVERY);
		List<CustomerOrder> orders = orderRepository.findByMenuIdAndCompanyIdAndStatusIn(menu.getId(), company.getId(), openStatuses);
		for (CustomerOrder order : orders) {
			order.setStatus(status);
			order.setUpdatedAt(Instant.now());
			User orderOwner = order.getCustomer() != null ? order.getCustomer() : order.getEmployee();
			if (orderOwner != null) {
				notificationService.notifyUser(
						orderOwner.getId(),
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

}
