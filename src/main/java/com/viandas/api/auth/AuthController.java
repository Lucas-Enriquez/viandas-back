package com.viandas.api.auth;

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
	AuthService.AuthResponse login(@Valid @RequestBody AuthService.LoginRequest request) {
		return authService.login(request);
	}

	@PostMapping("/auth/google")
	AuthService.AuthResponse google(@Valid @RequestBody AuthService.GoogleLoginRequest request) {
		return authService.googleLogin(request);
	}

	@PostMapping("/internal/bootstrap/cook")
	AuthService.AuthResponse bootstrapCook(@Valid @RequestBody AuthService.BootstrapCookRequest request) {
		return authService.bootstrapCook(request);
	}
}
