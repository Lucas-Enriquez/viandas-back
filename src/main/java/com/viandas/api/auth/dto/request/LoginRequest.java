package com.viandas.api.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "El mail es obligatorio")
        @Email(message = "El mail debe ser válido")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        String password
) {
}