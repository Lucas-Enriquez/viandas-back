package com.viandas.api.invitation.dto.response;

import java.time.Instant;

public record InvitationResponse(
        String token,
        String email,
        Instant expiresAt,
        String link
) {
}
