package com.viandas.api.auth;

import com.viandas.api.user.UserRole;

public record CurrentUser(Long userId, UserRole role) {
	public boolean isCook() {
		return role == UserRole.COOK;
	}

	public boolean isCustomer() {
		return role == UserRole.CUSTOMER;
	}
}
