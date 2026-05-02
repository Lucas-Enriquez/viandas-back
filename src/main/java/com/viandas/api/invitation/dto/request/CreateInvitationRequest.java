package com.viandas.api.invitation.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateInvitationRequest(
		@NotBlank(message = "El email es obligatorio")
		@Email(message = "Debe ingresar un email valido")
		String email) {
}
