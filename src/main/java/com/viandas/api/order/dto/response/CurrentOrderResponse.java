package com.viandas.api.order.dto.response;

public record CurrentOrderResponse(boolean hasOrder, boolean canOrder, String message, OrderResponse order) {
}
