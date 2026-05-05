package com.viandas.api.delivery.web;

import java.util.UUID;

import com.viandas.api.delivery.application.*;
import com.viandas.api.delivery.dto.request.LocationUpdateRequest;
import com.viandas.api.delivery.dto.request.StartDeliverySessionRequest;
import com.viandas.api.delivery.dto.response.DeliverySessionResponse;
import com.viandas.api.shared.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viandas.api.auth.security.SecurityUtils;

@RestController
@RequestMapping("/delivery-sessions")
public class DeliveryController {
	private final DeliveryService deliveryService;

	public DeliveryController(DeliveryService deliveryService) {
		this.deliveryService = deliveryService;
	}

	@PostMapping
	ApiResponse<DeliverySessionResponse> start(@Valid @RequestBody StartDeliverySessionRequest request) {
		return ApiResponse.ok("Reparto iniciado", deliveryService.start(SecurityUtils.currentUser(), request));
	}

	@PatchMapping("/{id}/location")
	ApiResponse<DeliverySessionResponse> updateLocation(@PathVariable UUID id, @Valid @RequestBody LocationUpdateRequest request) {
		return ApiResponse.ok("Ubicacion de reparto actualizada", deliveryService.updateLocation(SecurityUtils.currentUser(), id, request));
	}

	@PostMapping("/{id}/finish")
	ApiResponse<DeliverySessionResponse> finish(@PathVariable UUID id) {
		return ApiResponse.ok("Reparto finalizado", deliveryService.finish(SecurityUtils.currentUser(), id));
	}
}
