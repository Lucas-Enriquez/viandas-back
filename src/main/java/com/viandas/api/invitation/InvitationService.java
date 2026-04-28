package com.viandas.api.invitation;

import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.viandas.api.company.Company;
import com.viandas.api.company.CompanyMembership;
import com.viandas.api.company.CompanyMembershipRepository;
import com.viandas.api.company.CompanyService;
import com.viandas.api.shared.ApiException;
import com.viandas.api.user.User;
import com.viandas.api.user.UserRepository;
import com.viandas.api.user.UserRole;

@Service
public class InvitationService {
	private final InvitationRepository invitationRepository;
	private final CompanyService companyService;
	private final UserRepository userRepository;
	private final CompanyMembershipRepository companyMembershipRepository;
	private final PasswordEncoder passwordEncoder;
	private final String publicBaseUrl;

	public InvitationService(
			InvitationRepository invitationRepository,
			CompanyService companyService,
			UserRepository userRepository,
			CompanyMembershipRepository companyMembershipRepository,
			PasswordEncoder passwordEncoder,
			@Value("${viandas.public-base-url}") String publicBaseUrl) {
		this.invitationRepository = invitationRepository;
		this.companyService = companyService;
		this.userRepository = userRepository;
		this.companyMembershipRepository = companyMembershipRepository;
		this.passwordEncoder = passwordEncoder;
		this.publicBaseUrl = publicBaseUrl;
	}

	@Transactional
	public InvitationResponse create(com.viandas.api.auth.CurrentUser currentUser, Long companyId, CreateInvitationRequest request) {
		Company company = companyService.requireOwnedCompany(currentUser, companyId);
		Invitation invitation = invitationRepository.save(new Invitation(company, request.email(), Instant.now().plusSeconds(72 * 60 * 60)));
		return toResponse(invitation);
	}

	public InvitationValidationResponse validate(UUID token) {
		Invitation invitation = validInvitation(token);
		return new InvitationValidationResponse(invitation.getCompany().getName(), invitation.getEmail(), invitation.getExpiresAt());
	}

	@Transactional
	public AcceptInvitationResponse accept(UUID token, AcceptInvitationRequest request) {
		Invitation invitation = validInvitation(token);
		String email = invitation.getEmail() != null && !invitation.getEmail().isBlank()
				? invitation.getEmail().trim().toLowerCase()
				: request.email().trim().toLowerCase();
		if (userRepository.existsByEmailIgnoreCase(email)) {
			throw ApiException.conflict("Email already exists");
		}
		User employee = userRepository.save(new User(request.name().trim(), email, passwordEncoder.encode(request.password()), UserRole.EMPLOYEE));
		companyMembershipRepository.save(new CompanyMembership(invitation.getCompany(), employee));
		invitation.setUsed(true);
		return new AcceptInvitationResponse(employee.getId(), invitation.getCompany().getId());
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

	public record CreateInvitationRequest(String email) {
	}

	public record InvitationResponse(UUID token, String email, Instant expiresAt, String link) {
	}

	public record InvitationValidationResponse(String companyName, String email, Instant expiresAt) {
	}

	public record AcceptInvitationRequest(String name, String email, String password) {
	}

	public record AcceptInvitationResponse(Long userId, Long companyId) {
	}
}
