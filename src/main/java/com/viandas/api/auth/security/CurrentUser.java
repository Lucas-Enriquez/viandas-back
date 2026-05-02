package com.viandas.api.auth.security;

import com.viandas.api.user.domain.UserRole;

public record CurrentUser(Long userId, UserRole role) {
	public boolean isCook() {
		return role == UserRole.COOK;
	}

	public boolean isCustomer() {
		return role == UserRole.CUSTOMER;
	}
}
