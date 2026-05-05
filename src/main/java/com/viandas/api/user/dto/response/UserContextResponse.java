package com.viandas.api.user.dto.response;

import com.viandas.api.auth.dto.response.AuthUserResponse;

public record UserContextResponse(AuthUserResponse user, UserContextCompanyResponse company) {
}
