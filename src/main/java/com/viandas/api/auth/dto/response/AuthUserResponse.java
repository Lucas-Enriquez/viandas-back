package com.viandas.api.auth.dto.response;

import com.viandas.api.user.UserRole;

public record AuthUserResponse(
        Long id,
        String name,
        String email,
        UserRole role
) {
}