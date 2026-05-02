package com.viandas.api.notification.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterDeviceRequest(
		@NotBlank(message = "El token es obligatorio")
		@Size(max = 500, message = "El token no puede superar 500 caracteres")
		String token,

		@NotBlank(message = "La plataforma es obligatoria")
		@Size(max = 40, message = "La plataforma no puede superar 40 caracteres")
		String platform) {
}
