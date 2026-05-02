package com.viandas.api.menu.dto.request;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;

public record CreateMenuRequest(
		@NotNull(message = "La empresa es obligatoria")
		Long companyId,

		@NotNull(message = "La fecha es obligatoria")
		LocalDate date,

		@NotNull(message = "El horario de cierre es obligatorio")
		LocalTime orderClosesAt) {
}
