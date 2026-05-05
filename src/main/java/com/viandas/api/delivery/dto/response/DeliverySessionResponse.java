package com.viandas.api.delivery.dto.response;

import java.util.UUID;

import java.time.Instant;

import com.viandas.api.delivery.domain.DeliveryPublicSignal;
import com.viandas.api.delivery.domain.DeliverySessionStatus;

public record DeliverySessionResponse(
		UUID id,
		UUID companyId,
		UUID menuId,
		DeliverySessionStatus status,
		DeliveryPublicSignal publicSignal,
		Instant startedAt,
		Instant finishedAt,
		Instant expiresAt,
		Instant lastLocationAt) {
}
