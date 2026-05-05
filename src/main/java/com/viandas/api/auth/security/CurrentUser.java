package com.viandas.api.auth.security;

import java.util.UUID;

import com.viandas.api.user.domain.UserRole;

public record CurrentUser(UUID userId, UserRole role) {
	public boolean isCook() {
		return role == UserRole.COOK;
	}

	public boolean isCustomer() {
		return role == UserRole.CUSTOMER;
	}
}
