package com.viandas.api.auth.web;

import com.viandas.api.auth.application.AuthService;
import com.viandas.api.auth.dto.request.BootstrapCookRequest;
import com.viandas.api.auth.dto.response.AuthResponse;
import com.viandas.api.shared.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Solo disponible en perfil "dev". En producción este controller no existe.
 */
@Profile("dev")
@RestController
@RequestMapping
public class BootstrapController {

    private final AuthService authService;

    public BootstrapController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/internal/bootstrap/cook")
    ApiResponse<AuthResponse> bootstrapCook(@Valid @RequestBody BootstrapCookRequest request) {
        return ApiResponse.ok("Cook creado", authService.bootstrapCook(request));
    }
}
