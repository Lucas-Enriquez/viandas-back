package com.viandas.api.menu.dto.response;

import java.math.BigDecimal;

import com.viandas.api.menu.domain.MenuItemCategory;

public record MenuItemResponse(Long id, String name, BigDecimal price, MenuItemCategory category, String photoUrl, Integer remainingStock) {
}
