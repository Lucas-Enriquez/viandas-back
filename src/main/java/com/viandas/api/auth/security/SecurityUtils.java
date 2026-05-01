package com.viandas.api.auth.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.viandas.api.shared.ApiException;

public final class SecurityUtils {
	private SecurityUtils() {
	}

	public static CurrentUser currentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof CurrentUser currentUser)) {
			throw ApiException.unauthorized("Authentication required");
		}
		return currentUser;
	}
}
