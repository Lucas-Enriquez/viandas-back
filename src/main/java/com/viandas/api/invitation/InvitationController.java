package com.viandas.api.invitation;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viandas.api.auth.SecurityUtils;

@RestController
@RequestMapping
public class InvitationController {
	private final InvitationService invitationService;

	public InvitationController(InvitationService invitationService) {
		this.invitationService = invitationService;
	}

	@PostMapping("/companies/{id}/invitations")
	InvitationService.InvitationResponse create(@PathVariable Long id, @RequestBody InvitationService.CreateInvitationRequest request) {
		return invitationService.create(SecurityUtils.currentUser(), id, request);
	}

	@GetMapping("/invitations/{token}")
	InvitationService.InvitationValidationResponse validate(@PathVariable UUID token) {
		return invitationService.validate(token);
	}

	@PostMapping("/invitations/{token}/accept")
	InvitationService.AcceptInvitationResponse accept(@PathVariable UUID token, @RequestBody InvitationService.AcceptInvitationRequest request) {
		return invitationService.accept(token, request);
	}
}
