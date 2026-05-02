package com.viandas.api.menu.dto.request;

import java.math.BigDecimal;

import com.viandas.api.menu.domain.MenuItemCategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record AddMenuItemRequest(
		@NotBlank(message = "El nombre es obligatorio")
		@Size(max = 180, message = "El nombre no puede superar 180 caracteres")
		String name,

		@NotNull(message = "El precio es obligatorio")
		@Positive(message = "El precio debe ser positivo")
		BigDecimal price,

		@NotNull(message = "La categoria es obligatoria")
		MenuItemCategory category,

		@Size(max = 500, message = "La URL de la foto no puede superar 500 caracteres")
		String photoUrl,

		@PositiveOrZero(message = "El stock debe ser cero o positivo")
		Integer remainingStock) {
}
