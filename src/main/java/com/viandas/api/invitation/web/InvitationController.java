package com.viandas.api.invitation.web;

import com.viandas.api.invitation.application.*;
import com.viandas.api.invitation.dto.request.AcceptGlobalInvitationRequest;
import com.viandas.api.invitation.dto.request.AcceptInvitationRequest;
import com.viandas.api.invitation.dto.request.CreateGlobalInvitationRequest;
import com.viandas.api.invitation.dto.request.CreateInvitationRequest;
import com.viandas.api.invitation.dto.response.*;

import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viandas.api.auth.security.SecurityUtils;

@RestController
@RequestMapping
public class InvitationController {
    private final InvitationService invitationService;
    private final GlobalInvitationService globalInvitationService;

    public InvitationController(InvitationService invitationService, GlobalInvitationService globalInvitationService) {
        this.invitationService = invitationService;
        this.globalInvitationService = globalInvitationService;
    }

    @PostMapping("/companies/{id}/global-invitation")
    GlobalInvitationResponse create(@PathVariable Long id, @Valid @RequestBody CreateGlobalInvitationRequest request) {
        return globalInvitationService.create(SecurityUtils.currentUser(), id, request);
    }

    @PostMapping("/global-invitation/{token}/accept")
    AcceptGlobalInvitationResponse accept(
            @PathVariable String token,
            @Valid @RequestBody AcceptGlobalInvitationRequest request
    ) {
        return globalInvitationService.accept(token, request);
    }

    @PostMapping("/companies/{id}/invitations")
    InvitationResponse create(@PathVariable Long id, @Valid @RequestBody CreateInvitationRequest request) {
        return invitationService.create(SecurityUtils.currentUser(), id, request);
    }

    @GetMapping("/invitations/{token}")
    InvitationValidationResponse validate(@PathVariable UUID token) {
        return invitationService.validate(token);
    }

    @PostMapping("/invitations/{token}/accept")
    AcceptInvitationResponse accept(@PathVariable UUID token, @Valid @RequestBody AcceptInvitationRequest request) {
        return invitationService.accept(token, request);
    }
}
