package com.viandas.api.order.dto.response;

import java.util.UUID;

import java.time.Instant;
import java.util.List;

public record StockBroadcastResponse(UUID id, Instant sentAt, List<UUID> availableItemIds) {
}
