package com.viandas.api.product.dto.request;

import java.math.BigDecimal;

import com.viandas.api.menu.domain.MenuItemCategory;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Payload para actualizar parcialmente un producto (PATCH /products/{id}).
 * Cualquier campo en {@code null} significa "no tocar" — el valor actual se preserva.
 */
public record ProductPatchRequest(
		@Size(min = 1, max = 180, message = "El nombre no puede estar vacio ni superar 180 caracteres")
		String name,

		@Positive(message = "El precio debe ser positivo")
		BigDecimal price,

		MenuItemCategory category,

		@Size(max = 255, message = "El identificador de la foto no puede superar 255 caracteres")
		String photoPublicId,

		@Size(max = 500, message = "La descripcion no puede superar 500 caracteres")
		String description) {
}
