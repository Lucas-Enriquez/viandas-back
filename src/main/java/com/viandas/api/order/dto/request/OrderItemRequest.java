package com.viandas.api.order.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record OrderItemRequest(
		@NotNull(message = "El item del menu es obligatorio")
		Long menuItemId,

		@Positive(message = "La cantidad debe ser positiva")
		int quantity,

		@Size(max = 500, message = "El comentario no puede superar 500 caracteres")
		String comment) {
}
