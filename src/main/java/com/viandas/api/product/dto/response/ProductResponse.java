package com.viandas.api.product.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.viandas.api.menu.domain.MenuItemCategory;

public record ProductResponse(
		UUID id,
		String name,
		BigDecimal price,
		MenuItemCategory category,
		String photoUrl,
		String description,
		Instant createdAt,
		Instant updatedAt) {
}
