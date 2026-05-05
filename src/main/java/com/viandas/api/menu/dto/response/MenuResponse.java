package com.viandas.api.menu.dto.response;

import java.util.UUID;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.viandas.api.menu.domain.MenuScope;
import com.viandas.api.menu.domain.MenuStatus;

public record MenuResponse(
		UUID id,
		UUID companyId,
		String companyName,
		MenuScope scope,
		List<MenuCompanyResponse> companies,
		LocalDate date,
		MenuStatus status,
		LocalTime orderClosesAt,
		List<MenuItemResponse> items) {
}
