package com.viandas.api.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(
		@NotBlank(message = "El idToken es obligatorio")
		String idToken) {
}
