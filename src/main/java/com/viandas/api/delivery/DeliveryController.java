package com.viandas.api.delivery;

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
	DeliveryService.DeliverySessionResponse start(@RequestBody DeliveryService.StartDeliverySessionRequest request) {
		return deliveryService.start(SecurityUtils.currentUser(), request);
	}

	@PatchMapping("/{id}/location")
	DeliveryService.DeliverySessionResponse updateLocation(@PathVariable Long id, @RequestBody DeliveryService.LocationUpdateRequest request) {
		return deliveryService.updateLocation(SecurityUtils.currentUser(), id, request);
	}

	@PostMapping("/{id}/finish")
	DeliveryService.DeliverySessionResponse finish(@PathVariable Long id) {
		return deliveryService.finish(SecurityUtils.currentUser(), id);
	}
}
