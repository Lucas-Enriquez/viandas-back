package com.viandas.api.delivery.dto.request;

import jakarta.validation.constraints.NotNull;

public record StartDeliverySessionRequest(
		@NotNull(message = "La empresa es obligatoria")
		Long companyId,

		@NotNull(message = "El menu es obligatorio")
		Long menuId) {
}
