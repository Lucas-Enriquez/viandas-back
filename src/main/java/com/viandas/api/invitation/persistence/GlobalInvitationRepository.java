package com.viandas.api.invitation.persistence;

import com.viandas.api.invitation.domain.GlobalInvitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GlobalInvitationRepository extends JpaRepository<GlobalInvitation, UUID> {

    Optional<GlobalInvitation> findByTokenHash(String tokenHash);
    Optional<GlobalInvitation> findByTokenHashAndActiveTrue(String tokenHash);
    Optional<GlobalInvitation> findByCompanyId(UUID companyId);
    Optional<GlobalInvitation> findByCompanyIdAndActiveTrue(UUID companyId);
}
