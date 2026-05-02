package com.viandas.api.menu.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.viandas.api.menu.domain.MenuStatus;

public record MenuResponse(
		Long id,
		Long companyId,
		String companyName,
		LocalDate date,
		MenuStatus status,
		LocalTime orderClosesAt,
		List<MenuItemResponse> items) {
}
