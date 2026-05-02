package com.viandas.api.delivery.web;

import com.viandas.api.delivery.application.*;
import com.viandas.api.delivery.dto.request.LocationUpdateRequest;
import com.viandas.api.delivery.dto.request.StartDeliverySessionRequest;
import com.viandas.api.delivery.dto.response.DeliverySessionResponse;
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
	DeliverySessionResponse start(@Valid @RequestBody StartDeliverySessionRequest request) {
		return deliveryService.start(SecurityUtils.currentUser(), request);
	}

	@PatchMapping("/{id}/location")
	DeliverySessionResponse updateLocation(@PathVariable Long id, @Valid @RequestBody LocationUpdateRequest request) {
		return deliveryService.updateLocation(SecurityUtils.currentUser(), id, request);
	}

	@PostMapping("/{id}/finish")
	DeliverySessionResponse finish(@PathVariable Long id) {
		return deliveryService.finish(SecurityUtils.currentUser(), id);
	}
}
