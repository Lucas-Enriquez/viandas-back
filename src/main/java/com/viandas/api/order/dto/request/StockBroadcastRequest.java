package com.viandas.api.order.dto.request;

import java.util.UUID;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StockBroadcastRequest(
		@NotNull(message = "La empresa es obligatoria")
		UUID companyId,

		@NotNull(message = "El menu es obligatorio")
		UUID menuId,

		List<UUID> availableItemIds,

		@Size(max = 500, message = "El mensaje no puede superar 500 caracteres")
		String message) {
}
