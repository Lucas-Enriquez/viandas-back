package com.viandas.api.auth;

import com.viandas.api.auth.dto.request.BootstrapCookRequest;
import com.viandas.api.auth.dto.request.GoogleLoginRequest;
import com.viandas.api.auth.dto.request.LoginRequest;
import com.viandas.api.auth.dto.request.RefreshTokenRequest;
import com.viandas.api.auth.dto.response.AuthResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
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
    AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/auth/google")
    AuthResponse google(@Valid @RequestBody GoogleLoginRequest request) {
        return authService.googleLogin(request);
    }

    @PostMapping("/auth/refresh")
    AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
    }

    @PostMapping("/internal/bootstrap/cook")
    AuthResponse bootstrapCook(@Valid @RequestBody BootstrapCookRequest request) {
        return authService.bootstrapCook(request);
    }
}
