package com.viandas.api.product.dto.request;

import java.math.BigDecimal;

import com.viandas.api.menu.domain.MenuItemCategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Payload para crear un producto (POST /products).
 * Todos los campos relevantes son obligatorios.
 */
public record ProductCreateRequest(
		@NotBlank(message = "El nombre es obligatorio")
		@Size(max = 180, message = "El nombre no puede superar 180 caracteres")
		String name,

		@NotNull(message = "El precio es obligatorio")
		@Positive(message = "El precio debe ser positivo")
		BigDecimal price,

		@NotNull(message = "La categoria es obligatoria")
		MenuItemCategory category,

		@Size(max = 255, message = "El identificador de la foto no puede superar 255 caracteres")
		String photoPublicId,

		@Size(max = 500, message = "La descripcion no puede superar 500 caracteres")
		String description) {
}
