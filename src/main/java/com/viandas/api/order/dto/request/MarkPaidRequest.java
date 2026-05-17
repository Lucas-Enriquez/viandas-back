package com.viandas.api.order.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MarkPaidRequest(
		@NotNull(message = "El estado de pago es obligatorio")
		Boolean paid,

		@Size(max = 280, message = "La nota no puede superar los 280 caracteres")
		String note) {
}
