package com.viandas.api.menu.dto.response;

import java.util.UUID;

import java.math.BigDecimal;
import java.util.List;

import com.viandas.api.menu.domain.MenuItemCategory;

public record MenuItemResponse(
		UUID id,
		String name,
		BigDecimal price,
		MenuItemCategory category,
		String photoUrl,
		Integer remainingStock,
		List<UUID> availableCompanyIds) {
}
