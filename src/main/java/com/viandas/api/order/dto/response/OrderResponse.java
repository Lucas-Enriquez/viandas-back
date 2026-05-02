package com.viandas.api.order.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.viandas.api.delivery.domain.DeliveryPublicSignal;
import com.viandas.api.order.domain.OrderStatus;

public record OrderResponse(
		Long id,
		Long companyId,
		Long menuId,
		OrderStatus status,
		DeliveryPublicSignal deliverySignal,
		String customerName,
		BigDecimal totalAmount,
		Instant createdAt,
		List<OrderItemResponse> items) {
}
