package com.viandas.api.menu.dto.response;

import java.util.UUID;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

public record PublicMenuResponse(
		UUID id,
		String companyName,
		String companySlug,
		@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
		LocalDate date,
		LocalTime orderClosesAt,
		boolean canOrder,
		List<MenuItemResponse> items) {
}
