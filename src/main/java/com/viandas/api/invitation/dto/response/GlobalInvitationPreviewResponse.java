package com.viandas.api.invitation.dto.response;

import java.time.Instant;

public record GlobalInvitationPreviewResponse(
		String company,
		Instant expiresAt,
		boolean usable,
		Integer maxUses,
		int usedCount) {
}
