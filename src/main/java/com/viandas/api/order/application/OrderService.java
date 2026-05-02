package com.viandas.api.order.application;

import com.viandas.api.order.domain.*;
import com.viandas.api.order.dto.request.CreateOrderRequest;
import com.viandas.api.order.dto.request.OrderItemRequest;
import com.viandas.api.order.dto.request.StockBroadcastRequest;
import com.viandas.api.order.dto.response.CurrentOrderResponse;
import com.viandas.api.order.dto.response.OrderItemResponse;
import com.viandas.api.order.dto.response.OrderResponse;
import com.viandas.api.order.dto.response.StockBroadcastResponse;
import com.viandas.api.order.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.viandas.api.auth.security.CurrentUser;
import com.viandas.api.company.persistence.CompanyRepository;
import com.viandas.api.delivery.domain.DeliveryPublicSignal;
import com.viandas.api.delivery.domain.DeliverySession;
import com.viandas.api.delivery.persistence.DeliverySessionRepository;
import com.viandas.api.delivery.domain.DeliverySessionStatus;
import com.viandas.api.menu.domain.Menu;
import com.viandas.api.menu.domain.MenuItem;
import com.viandas.api.menu.persistence.MenuItemRepository;
import com.viandas.api.menu.application.MenuService;
import com.viandas.api.notification.application.NotificationService;
import com.viandas.api.notification.application.OrderEventBroadcaster;
import com.viandas.api.notification.domain.StockBroadcast;
import com.viandas.api.notification.domain.StockBroadcastItem;
import com.viandas.api.notification.persistence.StockBroadcastRepository;
import com.viandas.api.shared.ApiException;
import com.viandas.api.user.domain.User;
import com.viandas.api.user.persistence.UserRepository;
import com.viandas.api.user.domain.UserRole;

@Service
public class OrderService {
	private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Buenos_Aires");

	private final OrderRepository orderRepository;
	private final MenuItemRepository menuItemRepository;
	private final MenuService menuService;
	private final UserRepository userRepository;
	private final CompanyRepository companyRepository;
	private final DeliverySessionRepository deliverySessionRepository;
	private final NotificationService notificationService;
	private final OrderEventBroadcaster orderEventBroadcaster;
	private final StockBroadcastRepository stockBroadcastRepository;

	public OrderService(
			OrderRepository orderRepository,
			MenuItemRepository menuItemRepository,
			MenuService menuService,
			UserRepository userRepository,
			CompanyRepository companyRepository,
			DeliverySessionRepository deliverySessionRepository,
			NotificationService notificationService,
			OrderEventBroadcaster orderEventBroadcaster,
			StockBroadcastRepository stockBroadcastRepository) {
		this.orderRepository = orderRepository;
		this.menuItemRepository = menuItemRepository;
		this.menuService = menuService;
		this.userRepository = userRepository;
		this.companyRepository = companyRepository;
		this.deliverySessionRepository = deliverySessionRepository;
		this.notificationService = notificationService;
		this.orderEventBroadcaster = orderEventBroadcaster;
		this.stockBroadcastRepository = stockBroadcastRepository;
	}

	@Transactional
	public OrderResponse createPublicOrder(CurrentUser currentUser, String companySlug, LocalDate date, String token, CreateOrderRequest request) {
		if (currentUser.role() != UserRole.CUSTOMER) {
			throw ApiException.forbidden("Customer role required");
		}
		Menu menu = menuService.validatePublicAccess(companySlug, date, token).menu();
		if (!canOrder(menu)) {
			throw ApiException.conflict("Orders are closed for this menu");
		}
		User customer = userRepository.findById(currentUser.userId()).orElseThrow(() -> ApiException.unauthorized("User not found"));
		orderRepository.findFirstByMenuIdAndCustomerIdAndStatusNotOrderByCreatedAtDesc(menu.getId(), customer.getId(), OrderStatus.CANCELLED)
				.ifPresent(existing -> {
					throw ApiException.conflict("Customer already has an active order for this menu");
				});
		if (request.items() == null || request.items().isEmpty()) {
			throw ApiException.badRequest("Order must include at least one item");
		}
		Map<Long, MenuItem> menuItems = menuItemRepository.findAllById(request.items().stream().map(OrderItemRequest::menuItemId).toList()).stream()
				.collect(Collectors.toMap(MenuItem::getId, item -> item));
		CustomerOrder order = new CustomerOrder();
		order.setMenu(menu);
		order.setCompany(menu.getCompany());
		order.setCustomer(customer);
		order.setSource(OrderSource.CUSTOMER);
		order.setStatus(OrderStatus.RECEIVED);
		order.setCustomerNameSnapshot(customer.getName());
		List<OrderItem> orderItems = new ArrayList<>();
		BigDecimal total = BigDecimal.ZERO;
		for (OrderItemRequest itemRequest : request.items()) {
			MenuItem menuItem = menuItems.get(itemRequest.menuItemId());
			if (menuItem == null || !menuItem.getMenu().getId().equals(menu.getId())) {
				throw ApiException.badRequest("Invalid menu item");
			}
			if (itemRequest.quantity() <= 0) {
				throw ApiException.badRequest("Quantity must be positive");
			}
			if (menuItem.getRemainingStock() != null) {
				if (menuItem.getRemainingStock() < itemRequest.quantity()) {
					throw ApiException.conflict("Insufficient stock for " + menuItem.getName());
				}
				menuItem.setRemainingStock(menuItem.getRemainingStock() - itemRequest.quantity());
			}
			OrderItem orderItem = new OrderItem();
			orderItem.setOrder(order);
			orderItem.setMenuItem(menuItem);
			orderItem.setItemNameSnapshot(menuItem.getName());
			orderItem.setUnitPriceSnapshot(menuItem.getPrice());
			orderItem.setQuantity(itemRequest.quantity());
			orderItem.setComment(itemRequest.comment());
			orderItems.add(orderItem);
			total = total.add(menuItem.getPrice().multiply(BigDecimal.valueOf(itemRequest.quantity())));
		}
		order.setTotalAmount(total);
		order.getItems().addAll(orderItems);
		CustomerOrder saved = orderRepository.save(order);
		OrderResponse response = toResponse(saved, DeliveryPublicSignal.UNKNOWN);
		orderEventBroadcaster.publish(menu.getCompany().getId(), "order.created", response);
		return response;
	}

	public CurrentOrderResponse currentPublicOrder(CurrentUser currentUser, String companySlug, LocalDate date, String token) {
		if (currentUser.role() != UserRole.CUSTOMER) {
			throw ApiException.forbidden("Customer role required");
		}
		Menu menu = menuService.validatePublicAccess(companySlug, date, token).menu();
		var order = orderRepository.findFirstByMenuIdAndCustomerIdAndStatusNotOrderByCreatedAtDesc(menu.getId(), currentUser.userId(), OrderStatus.CANCELLED);
		if (order.isEmpty()) {
			boolean canOrder = canOrder(menu);
			return new CurrentOrderResponse(false, canOrder, canOrder ? "Todavia podes pedir." : "Pedidos cerrados. Volve mañana.", null);
		}
		DeliveryPublicSignal signal = deliverySignal(menu);
		OrderResponse response = toResponse(order.get(), signal);
		return new CurrentOrderResponse(true, false, messageFor(order.get().getStatus(), signal), response);
	}

	public List<OrderResponse> today(CurrentUser currentUser) {
		requireCook(currentUser);
		LocalDate today = LocalDate.now(BUSINESS_ZONE);
		Instant from = today.atStartOfDay(BUSINESS_ZONE).toInstant();
		Instant to = today.plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant();
		return orderRepository.findByCompanyCookIdAndCreatedAtBetweenOrderByCreatedAtDesc(currentUser.userId(), from, to).stream()
				.map(order -> toResponse(order, deliverySignal(order.getMenu())))
				.toList();
	}

	public SseEmitter stream(CurrentUser currentUser, Long companyId) {
		requireCook(currentUser);
		if (companyId != null) {
			companyRepository.findByIdAndCookId(companyId, currentUser.userId())
					.orElseThrow(() -> ApiException.notFound("Company not found"));
			return orderEventBroadcaster.subscribe(companyId);
		}
		List<Long> companyIds = companyRepository.findByCookIdOrderByName(currentUser.userId()).stream().map(company -> company.getId()).toList();
		if (companyIds.isEmpty()) {
			throw ApiException.notFound("Cook has no companies");
		}
		return orderEventBroadcaster.subscribe(companyIds);
	}

	@Transactional
	public OrderResponse markStatus(CurrentUser currentUser, Long orderId, OrderStatus status) {
		requireCook(currentUser);
		CustomerOrder order = orderRepository.findByIdAndCompanyCookId(orderId, currentUser.userId())
				.orElseThrow(() -> ApiException.notFound("Order not found"));
		order.setStatus(status);
		order.setUpdatedAt(Instant.now());
		OrderResponse response = toResponse(order, deliverySignal(order.getMenu()));
		orderEventBroadcaster.publish(order.getCompany().getId(), "order.updated", response);
		if (order.getCustomer() != null) {
			notificationService.notifyUser(order.getCustomer().getId(), "Pedido actualizado", messageFor(status, deliverySignal(order.getMenu())), Map.of("orderId", order.getId().toString()));
		}
		return response;
	}

	@Transactional
	public StockBroadcastResponse stockBroadcast(CurrentUser currentUser, StockBroadcastRequest request) {
		requireCook(currentUser);
		Menu menu = menuService.requireOwnedMenu(currentUser, request.menuId());
		if (!menu.getCompany().getId().equals(request.companyId())) {
			throw ApiException.badRequest("Menu does not belong to company");
		}
		Set<Long> itemIds = request.availableItemIds() == null ? Set.of() : Set.copyOf(request.availableItemIds());
		Map<Long, MenuItem> items = menuItemRepository.findAllById(itemIds).stream().collect(Collectors.toMap(MenuItem::getId, item -> item));
		StockBroadcast broadcast = new StockBroadcast();
		broadcast.setCompany(menu.getCompany());
		broadcast.setMenu(menu);
		broadcast.setSentBy(userRepository.findById(currentUser.userId()).orElseThrow());
		broadcast.setMessage(request.message());
		for (Long itemId : itemIds) {
			MenuItem item = items.get(itemId);
			if (item == null || !item.getMenu().getId().equals(menu.getId())) {
				throw ApiException.badRequest("Invalid menu item in broadcast");
			}
			StockBroadcastItem broadcastItem = new StockBroadcastItem();
			broadcastItem.setStockBroadcast(broadcast);
			broadcastItem.setMenuItem(item);
			broadcast.getItems().add(broadcastItem);
		}
		StockBroadcast saved = stockBroadcastRepository.save(broadcast);
		orderEventBroadcaster.publish(menu.getCompany().getId(), "stock.broadcast", new StockBroadcastResponse(saved.getId(), saved.getSentAt(), itemIds.stream().toList()));
		return new StockBroadcastResponse(saved.getId(), saved.getSentAt(), itemIds.stream().toList());
	}

	private boolean canOrder(Menu menu) {
		LocalDate today = LocalDate.now(BUSINESS_ZONE);
		if (menu.getMenuDate().isBefore(today)) {
			return false;
		}
		if (menu.getMenuDate().isAfter(today)) {
			return true;
		}
		return LocalTime.now(BUSINESS_ZONE).isBefore(menu.getOrderClosesAt());
	}

	private DeliveryPublicSignal deliverySignal(Menu menu) {
		return deliverySessionRepository.findFirstByMenuIdAndCompanyIdAndStatusOrderByStartedAtDesc(
						menu.getId(),
						menu.getCompany().getId(),
						DeliverySessionStatus.ACTIVE)
				.map(this::signalFromSession)
				.orElse(DeliveryPublicSignal.UNKNOWN);
	}

	private DeliveryPublicSignal signalFromSession(DeliverySession session) {
		if (session.getLastLocationAt() == null) {
			return DeliveryPublicSignal.OUT_FOR_DELIVERY;
		}
		return session.getLastLocationAt().isBefore(Instant.now().minusSeconds(20 * 60))
				? DeliveryPublicSignal.UNKNOWN
				: DeliveryPublicSignal.OUT_FOR_DELIVERY;
	}

	private String messageFor(OrderStatus status, DeliveryPublicSignal signal) {
		if (status == OrderStatus.NEARBY || signal == DeliveryPublicSignal.NEARBY) {
			return "Esta cerca.";
		}
		return switch (status) {
			case RECEIVED -> "Pedido recibido.";
			case PREPARING -> "Tu pedido esta en preparacion.";
			case OUT_FOR_DELIVERY -> "Tu pedido ya salio.";
			case DELIVERED -> "Tu pedido fue entregado.";
			case CANCELLED -> "Tu pedido fue cancelado.";
			case NEARBY -> "Esta cerca.";
		};
	}

	private OrderResponse toResponse(CustomerOrder order, DeliveryPublicSignal deliverySignal) {
		DeliveryPublicSignal effectiveSignal = switch (order.getStatus()) {
			case NEARBY -> DeliveryPublicSignal.NEARBY;
			case DELIVERED -> DeliveryPublicSignal.DELIVERED;
			case OUT_FOR_DELIVERY -> deliverySignal == DeliveryPublicSignal.UNKNOWN ? DeliveryPublicSignal.OUT_FOR_DELIVERY : deliverySignal;
			default -> deliverySignal;
		};
		List<OrderItemResponse> items = order.getItems().stream()
				.sorted(Comparator.comparing(OrderItem::getId))
				.map(item -> new OrderItemResponse(
						item.getMenuItem().getId(),
						item.getItemNameSnapshot(),
						item.getUnitPriceSnapshot(),
						item.getQuantity(),
						item.getComment()))
				.toList();
		return new OrderResponse(
				order.getId(),
				order.getCompany().getId(),
				order.getMenu().getId(),
				order.getStatus(),
				effectiveSignal,
				order.getCustomerNameSnapshot(),
				order.getTotalAmount(),
				order.getCreatedAt(),
				items);
	}

	private static void requireCook(CurrentUser currentUser) {
		if (!currentUser.isCook()) {
			throw ApiException.forbidden("Cook role required");
		}
	}

}
