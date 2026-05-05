package com.viandas.api.order.dto.response;

import java.util.UUID;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.viandas.api.delivery.domain.DeliveryPublicSignal;
import com.viandas.api.order.domain.OrderStatus;

public record OrderResponse(
		UUID id,
		UUID companyId,
		UUID menuId,
		OrderStatus status,
		DeliveryPublicSignal deliverySignal,
		String customerName,
		BigDecimal totalAmount,
		Instant createdAt,
		List<OrderItemResponse> items) {
}
