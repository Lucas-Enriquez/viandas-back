package com.viandas.api.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BootstrapCookRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre no puede contener más de 120 caracteres")
        String name,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "Debe ingresar un mail válido")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, message = "La contraseña debe contener al menos 8 caractéres")
        String password,

        @NotBlank(message = "El bootstrapKey es obligatorio")
        String bootstrapKey
) {
}