package com.viandas.api.company.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GeocodePreviewRequest(
		@NotBlank(message = "La direccion es obligatoria")
		@Size(max = 255, message = "La direccion no puede superar 255 caracteres")
		String address) {
}
