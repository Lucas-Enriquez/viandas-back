package com.viandas.api.invitation.dto.response;

import java.time.Instant;
import java.util.UUID;

public record InvitationResponse(
        UUID token,
        String email,
        Instant expiresAt,
        String link
) {
}
