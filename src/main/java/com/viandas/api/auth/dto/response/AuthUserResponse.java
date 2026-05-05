package com.viandas.api.auth.dto.response;

import java.util.UUID;

import com.viandas.api.user.domain.UserRole;

public record AuthUserResponse(
        UUID id,
        String name,
        String email,
        UserRole role
) {
}
