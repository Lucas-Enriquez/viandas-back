package com.viandas.api.invitation.dto.request;

import jakarta.validation.constraints.*;

public record CreateGlobalInvitationRequest(
        @Positive(message = "El número debe ser cero o positivo")
        Integer maxUses
) {
}
