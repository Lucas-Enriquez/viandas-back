package com.viandas.api.order.web;

import java.util.UUID;

import com.viandas.api.order.domain.*;
import com.viandas.api.order.application.*;
import com.viandas.api.order.dto.request.CreateOrderRequest;
import com.viandas.api.order.dto.request.StockBroadcastRequest;
import com.viandas.api.order.dto.request.UpdateOrderItemCommentRequest;
import com.viandas.api.order.dto.response.CurrentOrderResponse;
import com.viandas.api.order.dto.response.OrderResponse;
import com.viandas.api.order.dto.response.StockBroadcastResponse;
import com.viandas.api.shared.ApiResponse;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
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

	@PostMapping("/employee/menus/{date}/orders")
	ApiResponse<OrderResponse> createEmployeeOrder(
			@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@Valid @RequestBody CreateOrderRequest request) {
		return ApiResponse.ok("Pedido creado", orderService.createEmployeeOrder(SecurityUtils.currentUser(), date, request));
	}

	@GetMapping("/employee/menus/{date}/orders/current")
	ApiResponse<CurrentOrderResponse> currentEmployeeOrder(
			@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		return ApiResponse.ok("Pedido actual obtenido", orderService.currentEmployeeOrder(SecurityUtils.currentUser(), date));
	}

	@DeleteMapping("/employee/menus/{date}/orders/current")
	ApiResponse<OrderResponse> cancelEmployeeOrder(
			@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		return ApiResponse.ok("Pedido cancelado", orderService.cancelEmployeeOrder(SecurityUtils.currentUser(), date));
	}

	@PatchMapping("/employee/menus/{date}/orders/current/items/{itemId}")
	ApiResponse<OrderResponse> updateOrderItemComment(
			@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@PathVariable UUID itemId,
			@Valid @RequestBody UpdateOrderItemCommentRequest request) {
		return ApiResponse.ok("Comentario actualizado", orderService.updateEmployeeOrderItemComment(SecurityUtils.currentUser(), date, itemId, request));
	}

	@GetMapping("/orders/today")
	ApiResponse<List<OrderResponse>> today() {
		return ApiResponse.ok("Pedidos de hoy obtenidos", orderService.today(SecurityUtils.currentUser()));
	}

	@GetMapping("/orders/stream")
	SseEmitter stream(@RequestParam(required = false) UUID companyId) {
		return orderService.stream(SecurityUtils.currentUser(), companyId);
	}

	@PatchMapping("/orders/{id}/preparing")
	ApiResponse<OrderResponse> preparing(@PathVariable UUID id) {
		return ApiResponse.ok("Pedido marcado como en preparacion", orderService.markStatus(SecurityUtils.currentUser(), id, OrderStatus.PREPARING));
	}

	@PatchMapping("/orders/{id}/out-for-delivery")
	ApiResponse<OrderResponse> outForDelivery(@PathVariable UUID id) {
		return ApiResponse.ok("Pedido marcado como en reparto", orderService.markStatus(SecurityUtils.currentUser(), id, OrderStatus.OUT_FOR_DELIVERY));
	}

	@PatchMapping("/orders/{id}/delivered")
	ApiResponse<OrderResponse> delivered(@PathVariable UUID id) {
		return ApiResponse.ok("Pedido marcado como entregado", orderService.markStatus(SecurityUtils.currentUser(), id, OrderStatus.DELIVERED));
	}

	@PatchMapping("/orders/{id}/cancel")
	ApiResponse<OrderResponse> cancel(@PathVariable UUID id) {
		return ApiResponse.ok("Pedido cancelado", orderService.markStatus(SecurityUtils.currentUser(), id, OrderStatus.CANCELLED));
	}

	@PostMapping("/stock-broadcast")
	ApiResponse<StockBroadcastResponse> stockBroadcast(@Valid @RequestBody StockBroadcastRequest request) {
		return ApiResponse.ok("Aviso de stock enviado", orderService.stockBroadcast(SecurityUtils.currentUser(), request));
	}
}
