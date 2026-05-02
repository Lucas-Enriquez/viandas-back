package com.viandas.api.invitation.dto.response;

import java.time.Instant;

public record InvitationValidationResponse(String companyName, String email, Instant expiresAt) {
}
