package com.viandas.api.order;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.viandas.api.auth.SecurityUtils;

@RestController
@RequestMapping
public class OrderController {
	private final OrderService orderService;

	public OrderController(OrderService orderService) {
		this.orderService = orderService;
	}

	@PostMapping("/public/menus/{companySlug}/{date}/orders")
	OrderService.OrderResponse createPublicOrder(
			@PathVariable String companySlug,
			@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@RequestParam("t") String token,
			@RequestBody OrderService.CreateOrderRequest request) {
		return orderService.createPublicOrder(SecurityUtils.currentUser(), companySlug, date, token, request);
	}

	@GetMapping("/public/menus/{companySlug}/{date}/orders/current")
	OrderService.CurrentOrderResponse currentPublicOrder(
			@PathVariable String companySlug,
			@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@RequestParam("t") String token) {
		return orderService.currentPublicOrder(SecurityUtils.currentUser(), companySlug, date, token);
	}

	@GetMapping("/orders/today")
	List<OrderService.OrderResponse> today() {
		return orderService.today(SecurityUtils.currentUser());
	}

	@GetMapping("/orders/stream")
	SseEmitter stream(@RequestParam(required = false) Long companyId) {
		return orderService.stream(SecurityUtils.currentUser(), companyId);
	}

	@PatchMapping("/orders/{id}/preparing")
	OrderService.OrderResponse preparing(@PathVariable Long id) {
		return orderService.markStatus(SecurityUtils.currentUser(), id, OrderStatus.PREPARING);
	}

	@PatchMapping("/orders/{id}/out-for-delivery")
	OrderService.OrderResponse outForDelivery(@PathVariable Long id) {
		return orderService.markStatus(SecurityUtils.currentUser(), id, OrderStatus.OUT_FOR_DELIVERY);
	}

	@PatchMapping("/orders/{id}/delivered")
	OrderService.OrderResponse delivered(@PathVariable Long id) {
		return orderService.markStatus(SecurityUtils.currentUser(), id, OrderStatus.DELIVERED);
	}

	@PatchMapping("/orders/{id}/cancel")
	OrderService.OrderResponse cancel(@PathVariable Long id) {
		return orderService.markStatus(SecurityUtils.currentUser(), id, OrderStatus.CANCELLED);
	}

	@PostMapping("/stock-broadcast")
	OrderService.StockBroadcastResponse stockBroadcast(@RequestBody OrderService.StockBroadcastRequest request) {
		return orderService.stockBroadcast(SecurityUtils.currentUser(), request);
	}
}
