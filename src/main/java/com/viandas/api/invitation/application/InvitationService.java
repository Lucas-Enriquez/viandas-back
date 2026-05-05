package com.viandas.api.invitation.application;

import com.viandas.api.company.application.CompanyService;
import com.viandas.api.invitation.domain.*;
import com.viandas.api.invitation.dto.request.AcceptInvitationRequest;
import com.viandas.api.invitation.dto.request.CreateInvitationRequest;
import com.viandas.api.invitation.dto.response.InvitationResponse;
import com.viandas.api.invitation.dto.response.InvitationValidationResponse;
import com.viandas.api.invitation.persistence.*;
import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.viandas.api.company.domain.Company;
import com.viandas.api.company.domain.CompanyMembership;
import com.viandas.api.company.persistence.CompanyMembershipRepository;
import com.viandas.api.auth.application.AuthService;
import com.viandas.api.auth.dto.response.AuthResponse;
import com.viandas.api.shared.ApiException;
import com.viandas.api.user.domain.User;
import com.viandas.api.user.persistence.UserRepository;
import com.viandas.api.user.domain.UserRole;

@Service
public class InvitationService {
	private final InvitationRepository invitationRepository;
	private final com.viandas.api.company.application.CompanyService companyService;
	private final UserRepository userRepository;
	private final CompanyMembershipRepository companyMembershipRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthService authService;
	private final String publicBaseUrl;

	public InvitationService(
			InvitationRepository invitationRepository,
			CompanyService companyService,
			UserRepository userRepository,
			CompanyMembershipRepository companyMembershipRepository,
			PasswordEncoder passwordEncoder,
			AuthService authService,
			@Value("${viandas.public-base-url}") String publicBaseUrl) {
		this.invitationRepository = invitationRepository;
		this.companyService = companyService;
		this.userRepository = userRepository;
		this.companyMembershipRepository = companyMembershipRepository;
		this.passwordEncoder = passwordEncoder;
		this.authService = authService;
		this.publicBaseUrl = publicBaseUrl;
	}

	@Transactional
	public InvitationResponse create(com.viandas.api.auth.security.CurrentUser currentUser, UUID companyId, CreateInvitationRequest request) {
		Company company = companyService.requireOwnedCompany(currentUser, companyId);
		Invitation invitation = invitationRepository.save(new Invitation(company, normalizeEmail(request.email()), Instant.now().plusSeconds(72 * 60 * 60)));
		return toResponse(invitation);
	}

	@Transactional(readOnly = true)
	public InvitationValidationResponse validate(UUID token) {
		Invitation invitation = validInvitation(token);
		return new InvitationValidationResponse(invitation.getCompany().getName(), invitation.getEmail(), invitation.getExpiresAt());
	}

	@Transactional
	public AuthResponse accept(UUID token, AcceptInvitationRequest request) {
		Invitation invitation = validInvitation(token);
		String requestEmail = normalizeEmail(request.email());
		String email = invitation.getEmail() != null && !invitation.getEmail().isBlank()
				? normalizeEmail(invitation.getEmail())
				: requestEmail;
		if (!email.equals(requestEmail)) {
			throw ApiException.badRequest("Email does not match invitation");
		}
		if (userRepository.existsByEmailIgnoreCase(email)) {
			throw ApiException.conflict("Email already exists");
		}
		User employee = userRepository.save(new User(request.name().trim(), email, passwordEncoder.encode(request.password()), UserRole.EMPLOYEE));
		companyMembershipRepository.save(new CompanyMembership(invitation.getCompany(), employee));
		invitation.setUsed(true);
		return authService.createSession(employee);
	}

	private Invitation validInvitation(UUID token) {
		Invitation invitation = invitationRepository.findByToken(token)
				.orElseThrow(() -> ApiException.notFound("Invitation not found"));
		if (invitation.isUsed()) {
			throw ApiException.conflict("Invitation already used");
		}
		if (invitation.getExpiresAt().isBefore(Instant.now())) {
			throw ApiException.conflict("Invitation expired");
		}
		return invitation;
	}

	private InvitationResponse toResponse(Invitation invitation) {
		String link = publicBaseUrl + "/invite/" + invitation.getToken();
		return new InvitationResponse(invitation.getToken(), invitation.getEmail(), invitation.getExpiresAt(), link);
	}

	private static String normalizeEmail(String email) {
		return email.trim().toLowerCase();
	}

}
