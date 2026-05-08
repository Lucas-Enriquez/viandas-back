package com.viandas.api.invitation.web;

import com.viandas.api.auth.dto.response.AuthResponse;
import com.viandas.api.invitation.application.*;
import com.viandas.api.invitation.dto.request.AcceptGlobalInvitationRequest;
import com.viandas.api.invitation.dto.request.AcceptInvitationRequest;
import com.viandas.api.invitation.dto.request.CreateGlobalInvitationRequest;
import com.viandas.api.invitation.dto.request.CreateInvitationRequest;
import com.viandas.api.invitation.dto.response.*;
import com.viandas.api.shared.ApiResponse;

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
    ApiResponse<GlobalInvitationResponse> create(@PathVariable UUID id, @Valid @RequestBody CreateGlobalInvitationRequest request) {
        return ApiResponse.ok("Invitacion global creada", globalInvitationService.create(SecurityUtils.currentUser(), id, request));
    }

    @GetMapping("/global-invitation/{token}")
    ApiResponse<GlobalInvitationPreviewResponse> preview(@PathVariable String token) {
        return ApiResponse.ok("Invitacion global obtenida", globalInvitationService.preview(token));
    }

    @PostMapping("/global-invitation/{token}/accept")
    ApiResponse<AuthResponse> accept(
            @PathVariable String token,
            @Valid @RequestBody AcceptGlobalInvitationRequest request
    ) {
        return ApiResponse.ok("Invitacion global aceptada", globalInvitationService.accept(token, request));
    }

    @PostMapping("/companies/{id}/invitations")
    ApiResponse<InvitationResponse> create(@PathVariable UUID id, @Valid @RequestBody CreateInvitationRequest request) {
        return ApiResponse.ok("Invitacion creada", invitationService.create(SecurityUtils.currentUser(), id, request));
    }

    @GetMapping("/invitations/{token}")
    ApiResponse<InvitationValidationResponse> validate(@PathVariable String token) {
        return ApiResponse.ok("Invitacion obtenida", invitationService.validate(token));
    }

    @PostMapping("/invitations/{token}/accept")
    ApiResponse<AuthResponse> accept(@PathVariable String token, @Valid @RequestBody AcceptInvitationRequest request) {
        return ApiResponse.ok("Invitacion aceptada", invitationService.accept(token, request));
    }
}
