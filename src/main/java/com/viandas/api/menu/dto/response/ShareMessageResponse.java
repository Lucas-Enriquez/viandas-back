package com.viandas.api.menu.dto.response;

import java.util.UUID;

public record ShareMessageResponse(UUID publicLinkId, String publicUrl, String whatsappText) {
}
