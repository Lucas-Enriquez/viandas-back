package com.viandas.api.order.dto.response;

import java.util.UUID;

import java.math.BigDecimal;

public record OrderItemResponse(UUID menuItemId, String name, BigDecimal unitPrice, int quantity, String comment) {
}
