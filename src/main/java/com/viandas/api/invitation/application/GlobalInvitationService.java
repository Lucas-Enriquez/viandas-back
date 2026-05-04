package com.viandas.api.invitation.application;

import com.viandas.api.auth.security.CurrentUser;
import com.viandas.api.company.application.CompanyService;
import com.viandas.api.company.domain.Company;
import com.viandas.api.company.domain.CompanyMembership;
import com.viandas.api.company.persistence.CompanyMembershipRepository;
import com.viandas.api.invitation.domain.GlobalInvitation;
import com.viandas.api.invitation.dto.request.AcceptGlobalInvitationRequest;
import com.viandas.api.invitation.dto.request.CreateGlobalInvitationRequest;
import com.viandas.api.invitation.dto.response.AcceptGlobalInvitationResponse;
import com.viandas.api.invitation.dto.response.GlobalInvitationResponse;
import com.viandas.api.invitation.persistence.GlobalInvitationRepository;
import com.viandas.api.shared.ApiException;
import com.viandas.api.shared.TokenHasher;
import com.viandas.api.user.domain.User;
import com.viandas.api.user.domain.UserRole;
import com.viandas.api.user.persistence.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class GlobalInvitationService {

    private final GlobalInvitationRepository globalInvitationRepository;
    private final CompanyService companyService;
    private final TokenHasher tokenHasher;
    private final String publicUrl;
    private final UserRepository userRepository;
    private final CompanyMembershipRepository companyMembershipRepository;
    private final PasswordEncoder passwordEncoder;

    public GlobalInvitationService(
            GlobalInvitationRepository globalInvitationRepository,
            CompanyService invitationService,
            TokenHasher tokenHasher,
            @Value("${viandas.public-base-url}") String publicBaseUrl,
            UserRepository userRepository,
            CompanyMembershipRepository companyMembershipRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.globalInvitationRepository = globalInvitationRepository;
        this.companyService = invitationService;
        this.tokenHasher = tokenHasher;
        this.publicUrl = publicBaseUrl;
        this.userRepository = userRepository;
        this.companyMembershipRepository = companyMembershipRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public GlobalInvitationResponse create(
            CurrentUser currentUser,
            Long companyId,
            CreateGlobalInvitationRequest request
    ) {
        Company company = companyService.requireOwnedCompany(currentUser, companyId);

        revokeExistingInvitation(companyId);

        GlobalInvitation invitation = new GlobalInvitation(
                company,
                Instant.now().plusSeconds(72 * 60 * 60)
        );

        String token = tokenHasher.newToken();
        String tokenHash = tokenHasher.hash(token);

        invitation.setTokenHash(tokenHash);
        invitation.setMaxUses(request.maxUses());

        GlobalInvitation saved = globalInvitationRepository.save(invitation);

        return toResponse(saved, token);
    }

    private void revokeExistingInvitation(Long companyId) {

        globalInvitationRepository.findByCompanyIdAndActiveTrue(companyId)
                .ifPresent(globalInvitation -> {
                    globalInvitation.setActive(false);
                    globalInvitation.setRevokedAt(Instant.now());
                });
    }

    @Transactional
    public AcceptGlobalInvitationResponse accept(String token, AcceptGlobalInvitationRequest request) {
        String tokenHash = tokenHasher.hash(token);

        GlobalInvitation globalInvitation = globalInvitationRepository.findByTokenHashAndActiveTrue(tokenHash)
                .orElseThrow(() -> ApiException.notFound("Invitación global inválida"));

        Instant now = Instant.now();

        if (globalInvitation.getExpiresAt().isBefore(now)) {
            throw ApiException.conflict("Invitación expirada");
        }

        if (globalInvitation.getMaxUses() != null && globalInvitation.getUsedCount() >= globalInvitation.getMaxUses()) {
            throw ApiException.conflict("La invitación alcanzó su límite de usos");
        }

        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("El email ya está en uso");
        }

        User employee = userRepository.save(
                new User(
                        request.name().trim(),
                        email,
                        passwordEncoder.encode(request.password()),
                        UserRole.EMPLOYEE
                )
        );

        companyMembershipRepository.save(
                new CompanyMembership(globalInvitation.getCompany(), employee)
        );

        globalInvitation.setUsedCount(globalInvitation.getUsedCount() + 1);

        if (globalInvitation.getMaxUses() != null && globalInvitation.getUsedCount() >= globalInvitation.getMaxUses()) {
            globalInvitation.setActive(false);
        }

        return new AcceptGlobalInvitationResponse(employee.getId(), globalInvitation.getCompany().getId());
    }


    private GlobalInvitationResponse toResponse(GlobalInvitation invitation, String token) {
        String link = publicUrl + "/global-invitation/" + token + "/accept";

        return new GlobalInvitationResponse(
                token,
                invitation.getCompany().getName(),
                invitation.getExpiresAt(),
                link
        );
    }

}
