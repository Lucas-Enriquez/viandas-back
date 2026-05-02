package com.viandas.api.delivery.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record LocationUpdateRequest(
		@NotNull(message = "La latitud es obligatoria")
		@DecimalMin(value = "-90", message = "La latitud debe ser mayor o igual a -90")
		@DecimalMax(value = "90", message = "La latitud debe ser menor o igual a 90")
		BigDecimal latitude,

		@NotNull(message = "La longitud es obligatoria")
		@DecimalMin(value = "-180", message = "La longitud debe ser mayor o igual a -180")
		@DecimalMax(value = "180", message = "La longitud debe ser menor o igual a 180")
		BigDecimal longitude,

		@PositiveOrZero(message = "La precision debe ser cero o positiva")
		BigDecimal accuracyMeters) {
}
