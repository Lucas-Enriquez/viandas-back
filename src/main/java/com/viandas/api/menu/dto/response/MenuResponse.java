package com.viandas.api.menu.dto.response;

import java.util.UUID;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.viandas.api.menu.domain.MenuScope;
import com.viandas.api.menu.domain.MenuStatus;

public record MenuResponse(
		UUID id,
		UUID companyId,
		String companyName,
		MenuScope scope,
		List<MenuCompanyResponse> companies,
		@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
		LocalDate date,
		MenuStatus status,
		LocalTime orderClosesAt,
		List<MenuItemResponse> items) {
}
