package com.viandas.api.user.dto.response;

import java.util.UUID;

public record UserContextCompanyResponse(UUID id, String name, String slug) {
}
