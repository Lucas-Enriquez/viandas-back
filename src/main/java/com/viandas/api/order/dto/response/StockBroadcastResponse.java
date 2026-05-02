package com.viandas.api.order.dto.response;

import java.time.Instant;
import java.util.List;

public record StockBroadcastResponse(Long id, Instant sentAt, List<Long> availableItemIds) {
}
