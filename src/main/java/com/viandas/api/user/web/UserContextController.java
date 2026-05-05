package com.viandas.api.user.web;

import com.viandas.api.auth.security.SecurityUtils;
import com.viandas.api.shared.ApiResponse;
import com.viandas.api.user.application.UserContextService;
import com.viandas.api.user.dto.response.UserContextResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me")
public class UserContextController {
	private final UserContextService userContextService;

	public UserContextController(UserContextService userContextService) {
		this.userContextService = userContextService;
	}

	@GetMapping("/context")
	ApiResponse<UserContextResponse> context() {
		return ApiResponse.ok("Contexto obtenido", userContextService.context(SecurityUtils.currentUser()));
	}
}
