package com.viandas.api.company.dto.response;

import java.util.UUID;

import java.math.BigDecimal;

import com.viandas.api.company.domain.LocationSource;

public record CompanyResponse(
		UUID id,
		String name,
		String slug,
		String address,
		String notes,
		BigDecimal latitude,
		BigDecimal longitude,
		LocationSource locationSource,
		String whatsappGroupLabel) {
}
