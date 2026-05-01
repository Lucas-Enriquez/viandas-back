package com.viandas.api.auth.dto.response;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        AuthUserResponse user
) {
}
