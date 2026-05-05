package com.viandas.api.menu.dto.request;

import java.util.UUID;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.viandas.api.menu.domain.MenuScope;

import jakarta.validation.constraints.NotNull;

public record CreateMenuRequest(
		MenuScope scope,

		UUID companyId,

		List<UUID> companyIds,

		@NotNull(message = "La fecha es obligatoria")
		LocalDate date,

		@NotNull(message = "El horario de cierre es obligatorio")
		LocalTime orderClosesAt) {
}
