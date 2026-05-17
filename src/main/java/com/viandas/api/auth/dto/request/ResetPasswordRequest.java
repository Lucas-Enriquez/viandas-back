package com.viandas.api.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
		@NotBlank(message = "El token es obligatorio")
		String token,

		@NotBlank(message = "La contrasena es obligatoria")
		@Size(min = 8, max = 100, message = "La contrasena debe tener entre 8 y 100 caracteres")
		String newPassword) {
}
