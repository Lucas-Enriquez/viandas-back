package com.viandas.api.company.dto.request;

import java.math.BigDecimal;

import com.viandas.api.company.domain.LocationSource;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

public record CompanyLocationRequest(
		@Size(max = 255, message = "La direccion no puede superar 255 caracteres")
		String address,

		@DecimalMin(value = "-90", message = "La latitud debe ser mayor o igual a -90")
		@DecimalMax(value = "90", message = "La latitud debe ser menor o igual a 90")
		BigDecimal latitude,

		@DecimalMin(value = "-180", message = "La longitud debe ser mayor o igual a -180")
		@DecimalMax(value = "180", message = "La longitud debe ser menor o igual a 180")
		BigDecimal longitude,

		LocationSource locationSource) {
}
