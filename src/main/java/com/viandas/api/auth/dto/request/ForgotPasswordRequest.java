package com.viandas.api.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
		@NotBlank(message = "El email es obligatorio")
		@Email(message = "El email no es valido")
		String email) {
}
