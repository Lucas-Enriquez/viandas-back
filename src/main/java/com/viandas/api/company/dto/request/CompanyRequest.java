package com.viandas.api.company.dto.request;

import java.math.BigDecimal;

import com.viandas.api.company.domain.LocationSource;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompanyRequest(
		@NotBlank(message = "El nombre es obligatorio")
		@Size(max = 160, message = "El nombre no puede superar 160 caracteres")
		String name,

		@Size(max = 255, message = "La direccion no puede superar 255 caracteres")
		String address,

		@Size(max = 2000, message = "Las notas no pueden superar 2000 caracteres")
		String notes,

		@DecimalMin(value = "-90", message = "La latitud debe ser mayor o igual a -90")
		@DecimalMax(value = "90", message = "La latitud debe ser menor o igual a 90")
		BigDecimal latitude,

		@DecimalMin(value = "-180", message = "La longitud debe ser mayor o igual a -180")
		@DecimalMax(value = "180", message = "La longitud debe ser menor o igual a 180")
		BigDecimal longitude,

		LocationSource locationSource,

		@Size(max = 160, message = "La etiqueta de WhatsApp no puede superar 160 caracteres")
		String whatsappGroupLabel) {
}
