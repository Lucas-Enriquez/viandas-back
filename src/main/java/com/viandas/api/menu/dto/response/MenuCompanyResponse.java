package com.viandas.api.menu.dto.response;

import java.util.UUID;

public record MenuCompanyResponse(UUID id, String name, String slug) {
}
