package com.viandas.api.invitation.application;

import com.viandas.api.auth.application.AuthService;
import com.viandas.api.auth.dto.response.AuthResponse;
import com.viandas.api.auth.security.CurrentUser;
import com.viandas.api.company.application.CompanyService;
import com.viandas.api.company.domain.Company;
import com.viandas.api.company.domain.CompanyMembership;
import com.viandas.api.company.persistence.CompanyMembershipRepository;
import com.viandas.api.invitation.domain.GlobalInvitation;
import com.viandas.api.invitation.dto.request.AcceptGlobalInvitationRequest;
import com.viandas.api.invitation.dto.request.CreateGlobalInvitationRequest;
import com.viandas.api.invitation.dto.response.GlobalInvitationPreviewResponse;
import com.viandas.api.invitation.dto.response.GlobalInvitationResponse;
import com.viandas.api.invitation.persistence.GlobalInvitationRepository;
import com.viandas.api.shared.ApiException;
import com.viandas.api.shared.TokenHasher;
import com.viandas.api.user.domain.User;
import com.viandas.api.user.domain.UserRole;
import com.viandas.api.user.persistence.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GlobalInvitationService {

	private final GlobalInvitationRepository globalInvitationRepository;
	private final CompanyService companyService;
	private final TokenHasher tokenHasher;
	private final String publicUrl;
	private final UserRepository userRepository;
	private final CompanyMembershipRepository companyMembershipRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthService authService;

	public GlobalInvitationService(
			GlobalInvitationRepository globalInvitationRepository,
			CompanyService companyService,
			TokenHasher tokenHasher,
			@Value("${viandas.public-base-url}") String publicBaseUrl,
			UserRepository userRepository,
			CompanyMembershipRepository companyMembershipRepository,
			PasswordEncoder passwordEncoder,
			AuthService authService) {
		this.globalInvitationRepository = globalInvitationRepository;
		this.companyService = companyService;
		this.tokenHasher = tokenHasher;
		this.publicUrl = publicBaseUrl;
		this.userRepository = userRepository;
		this.companyMembershipRepository = companyMembershipRepository;
		this.passwordEncoder = passwordEncoder;
		this.authService = authService;
	}

	@Transactional
	public GlobalInvitationResponse create(CurrentUser currentUser, UUID companyId, CreateGlobalInvitationRequest request) {
		Company company = companyService.requireOwnedCompany(currentUser, companyId);
		revokeExistingInvitation(companyId);

		GlobalInvitation invitation = new GlobalInvitation(company, Instant.now().plusSeconds(72 * 60 * 60));
		String token = tokenHasher.newToken();
		invitation.setTokenHash(tokenHasher.hash(token));
		invitation.setMaxUses(request.maxUses());

		GlobalInvitation saved = globalInvitationRepository.save(invitation);
		return toResponse(saved, token);
	}

	@Transactional(readOnly = true)
	public GlobalInvitationPreviewResponse preview(String token) {
		GlobalInvitation invitation = requireByToken(token);
		return new GlobalInvitationPreviewResponse(
				invitation.getCompany().getName(),
				invitation.getExpiresAt(),
				isUsable(invitation, Instant.now()),
				invitation.getMaxUses(),
				invitation.getUsedCount());
	}

	@Transactional
	public AuthResponse accept(String token, AcceptGlobalInvitationRequest request) {
		GlobalInvitation invitation = requireByToken(token);
		Instant now = Instant.now();

		if (!invitation.isActive()) {
			throw ApiException.conflict("Invitacion global inactiva");
		}
		if (invitation.getExpiresAt().isBefore(now)) {
			throw ApiException.conflict("Invitacion expirada");
		}
		if (invitation.getMaxUses() != null && invitation.getUsedCount() >= invitation.getMaxUses()) {
			throw ApiException.conflict("La invitacion alcanzo su limite de usos");
		}

		String email = normalizeEmail(request.email());
		if (userRepository.existsByEmailIgnoreCase(email)) {
			throw ApiException.conflict("El email ya esta en uso");
		}

		User employee = userRepository.save(new User(
				request.name().trim(),
				email,
				passwordEncoder.encode(request.password()),
				UserRole.EMPLOYEE));
		companyMembershipRepository.save(new CompanyMembership(invitation.getCompany(), employee));

		invitation.setUsedCount(invitation.getUsedCount() + 1);
		if (invitation.getMaxUses() != null && invitation.getUsedCount() >= invitation.getMaxUses()) {
			invitation.setActive(false);
		}

		return authService.createSession(employee);
	}

	private void revokeExistingInvitation(UUID companyId) {
		globalInvitationRepository.findByCompanyIdAndActiveTrue(companyId)
				.ifPresent(globalInvitation -> {
					globalInvitation.setActive(false);
					globalInvitation.setRevokedAt(Instant.now());
				});
	}

	private GlobalInvitationResponse toResponse(GlobalInvitation invitation, String token) {
		String link = publicUrl + "/global-invitation/" + token;
		return new GlobalInvitationResponse(
				token,
				invitation.getCompany().getName(),
				invitation.getExpiresAt(),
				link);
	}

	private GlobalInvitation requireByToken(String token) {
		return globalInvitationRepository.findByTokenHash(tokenHasher.hash(token))
				.orElseThrow(() -> ApiException.notFound("Invitacion global invalida"));
	}

	private static boolean isUsable(GlobalInvitation invitation, Instant now) {
		return invitation.isActive()
				&& !invitation.getExpiresAt().isBefore(now)
				&& (invitation.getMaxUses() == null || invitation.getUsedCount() < invitation.getMaxUses());
	}

	private static String normalizeEmail(String email) {
		return email.trim().toLowerCase();
	}
}
