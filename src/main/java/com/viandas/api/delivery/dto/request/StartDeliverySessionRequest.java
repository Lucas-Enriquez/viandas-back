package com.viandas.api.delivery.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record StartDeliverySessionRequest(
		@NotNull(message = "La empresa es obligatoria")
		UUID companyId,

		@NotNull(message = "El menu es obligatorio")
		UUID menuId) {
}
