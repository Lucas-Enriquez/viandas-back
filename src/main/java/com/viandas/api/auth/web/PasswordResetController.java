package com.viandas.api.auth.web;

import com.viandas.api.auth.application.PasswordResetService;
import com.viandas.api.auth.dto.request.ForgotPasswordRequest;
import com.viandas.api.auth.dto.request.ResetPasswordRequest;
import com.viandas.api.shared.ApiResponse;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PasswordResetController {

	private final PasswordResetService passwordResetService;

	public PasswordResetController(PasswordResetService passwordResetService) {
		this.passwordResetService = passwordResetService;
	}

	@PostMapping("/auth/forgot-password")
	ApiResponse<Void> forgot(@Valid @RequestBody ForgotPasswordRequest request) {
		passwordResetService.requestReset(request);
		return ApiResponse.ok("Si el email existe, se envio un correo con instrucciones", null);
	}

	@PostMapping("/auth/reset-password")
	ApiResponse<Void> reset(@Valid @RequestBody ResetPasswordRequest request) {
		passwordResetService.confirmReset(request);
		return ApiResponse.ok("Contrasena actualizada", null);
	}
}
