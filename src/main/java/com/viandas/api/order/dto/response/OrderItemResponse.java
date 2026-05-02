package com.viandas.api.order.dto.response;

import java.math.BigDecimal;

public record OrderItemResponse(Long menuItemId, String name, BigDecimal unitPrice, int quantity, String comment) {
}
