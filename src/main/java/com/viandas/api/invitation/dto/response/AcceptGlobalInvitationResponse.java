package com.viandas.api.invitation.dto.response;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AcceptGlobalInvitationResponse(

        Long userId,
        Long companyId
) {
}
