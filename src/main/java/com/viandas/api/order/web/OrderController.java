package com.viandas.api.order.web;

import com.viandas.api.order.domain.*;
import com.viandas.api.order.application.*;
import com.viandas.api.order.dto.request.CreateOrderRequest;
import com.viandas.api.order.dto.request.StockBroadcastRequest;
import com.viandas.api.order.dto.response.CurrentOrderResponse;
import com.viandas.api.order.dto.response.OrderResponse;
import com.viandas.api.order.dto.response.StockBroadcastResponse;
import com.viandas.api.shared.ApiResponse;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
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

import com.viandas.api.auth.security.SecurityUtils;

@RestController
@RequestMapping
public class OrderController {
	private final OrderService orderService;

	public OrderController(OrderService orderService) {
		this.orderService = orderService;
	}

	@PostMapping("/public/menus/{companySlug}/{date}/orders")
	ApiResponse<OrderResponse> createPublicOrder(
			@PathVariable String companySlug,
			@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@RequestParam("t") String token,
			@Valid @RequestBody CreateOrderRequest request) {
		return ApiResponse.ok("Pedido creado", orderService.createPublicOrder(SecurityUtils.currentUser(), companySlug, date, token, request));
	}

	@GetMapping("/public/menus/{companySlug}/{date}/orders/current")
	ApiResponse<CurrentOrderResponse> currentPublicOrder(
			@PathVariable String companySlug,
			@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@RequestParam("t") String token) {
		return ApiResponse.ok("Pedido actual obtenido", orderService.currentPublicOrder(SecurityUtils.currentUser(), companySlug, date, token));
	}

	@GetMapping("/orders/today")
	ApiResponse<List<OrderResponse>> today() {
		return ApiResponse.ok("Pedidos de hoy obtenidos", orderService.today(SecurityUtils.currentUser()));
	}

	@GetMapping("/orders/stream")
	SseEmitter stream(@RequestParam(required = false) Long companyId) {
		return orderService.stream(SecurityUtils.currentUser(), companyId);
	}

	@PatchMapping("/orders/{id}/preparing")
	ApiResponse<OrderResponse> preparing(@PathVariable Long id) {
		return ApiResponse.ok("Pedido marcado como en preparacion", orderService.markStatus(SecurityUtils.currentUser(), id, OrderStatus.PREPARING));
	}

	@PatchMapping("/orders/{id}/out-for-delivery")
	ApiResponse<OrderResponse> outForDelivery(@PathVariable Long id) {
		return ApiResponse.ok("Pedido marcado como en reparto", orderService.markStatus(SecurityUtils.currentUser(), id, OrderStatus.OUT_FOR_DELIVERY));
	}

	@PatchMapping("/orders/{id}/delivered")
	ApiResponse<OrderResponse> delivered(@PathVariable Long id) {
		return ApiResponse.ok("Pedido marcado como entregado", orderService.markStatus(SecurityUtils.currentUser(), id, OrderStatus.DELIVERED));
	}

	@PatchMapping("/orders/{id}/cancel")
	ApiResponse<OrderResponse> cancel(@PathVariable Long id) {
		return ApiResponse.ok("Pedido cancelado", orderService.markStatus(SecurityUtils.currentUser(), id, OrderStatus.CANCELLED));
	}

	@PostMapping("/stock-broadcast")
	ApiResponse<StockBroadcastResponse> stockBroadcast(@Valid @RequestBody StockBroadcastRequest request) {
		return ApiResponse.ok("Aviso de stock enviado", orderService.stockBroadcast(SecurityUtils.currentUser(), request));
	}
}
