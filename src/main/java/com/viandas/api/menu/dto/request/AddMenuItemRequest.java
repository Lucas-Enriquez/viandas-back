package com.viandas.api.menu.dto.request;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.viandas.api.menu.domain.MenuItemCategory;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Two flavours of "add item":
 * <ul>
 *     <li><b>Desde inventario</b>: enviar {@code productId}. El backend snapshotea
 *         {@code name/price/category/photoUrl} desde el Product. Los campos sueltos no deben venir.</li>
 *     <li><b>Free-form</b>: omitir {@code productId} y enviar {@code name/price/category}
 *         (obligatorios). Opcionalmente {@code photoPublicId} si se subió una foto a Cloudinary.</li>
 * </ul>
 * La validación cruzada de "uno u otro" se hace en {@code MenuService.addItem}.
 */
public record AddMenuItemRequest(
		UUID productId,

		@Size(max = 180, message = "El nombre no puede superar 180 caracteres")
		String name,

		@Positive(message = "El precio debe ser positivo")
		BigDecimal price,

		MenuItemCategory category,

		@Size(max = 255, message = "El identificador de la foto no puede superar 255 caracteres")
		String photoPublicId,

		@PositiveOrZero(message = "El stock debe ser cero o positivo")
		Integer remainingStock,

		List<UUID> availableCompanyIds) {
}
