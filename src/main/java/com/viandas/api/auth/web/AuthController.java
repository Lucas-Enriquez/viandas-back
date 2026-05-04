package com.viandas.api.auth.web;

import com.viandas.api.auth.application.AuthService;
import com.viandas.api.auth.dto.request.BootstrapCookRequest;
import com.viandas.api.auth.dto.request.GoogleLoginRequest;
import com.viandas.api.auth.dto.request.LoginRequest;
import com.viandas.api.auth.dto.request.RefreshTokenRequest;
import com.viandas.api.auth.dto.response.AuthResponse;
import com.viandas.api.shared.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/auth/login")
    ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok("Sesion iniciada", authService.login(request));
    }

    @PostMapping("/auth/google")
    ApiResponse<AuthResponse> google(@Valid @RequestBody GoogleLoginRequest request) {
        return ApiResponse.ok("Sesion iniciada con Google", authService.googleLogin(request));
    }

    @PostMapping("/auth/refresh")
    ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.ok("Sesion renovada", authService.refresh(request));
    }

    @PostMapping("/auth/logout")
    ApiResponse<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return ApiResponse.ok("Sesion cerrada", null);
    }

    @PostMapping("/internal/bootstrap/cook")
    ApiResponse<AuthResponse> bootstrapCook(@Valid @RequestBody BootstrapCookRequest request) {
        return ApiResponse.ok("Cook creado", authService.bootstrapCook(request));
    }
}
