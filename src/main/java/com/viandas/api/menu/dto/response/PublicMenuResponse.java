package com.viandas.api.menu.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record PublicMenuResponse(
		Long id,
		String companyName,
		String companySlug,
		LocalDate date,
		LocalTime orderClosesAt,
		boolean canOrder,
		List<MenuItemResponse> items) {
}
