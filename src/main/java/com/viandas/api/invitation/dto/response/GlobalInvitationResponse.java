package com.viandas.api.invitation.dto.response;

import java.time.Instant;
import java.util.UUID;

public record GlobalInvitationResponse(
        String token,
        String company,
        Instant expiresAt,
        String link
) {
}
