package com.viandas.api.notification.dto.response;

import java.util.UUID;

import java.time.Instant;

public record DeviceResponse(UUID id, String platform, Instant lastSeenAt) {
}
