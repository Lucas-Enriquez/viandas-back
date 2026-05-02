package com.viandas.api.order.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record CreateOrderRequest(
		@NotEmpty(message = "El pedido debe incluir al menos un item")
		List<@Valid OrderItemRequest> items) {
}
