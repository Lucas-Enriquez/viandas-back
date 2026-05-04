package com.viandas.api.invitation.persistence;

import com.viandas.api.invitation.domain.GlobalInvitation;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.swing.text.html.Option;
import java.util.Optional;
import java.util.UUID;

public interface GlobalInvitationRepository extends JpaRepository<GlobalInvitation, UUID> {

    Optional<GlobalInvitation> findByTokenHashAndActiveTrue(String tokenHash);
    Optional<GlobalInvitation> findByCompanyId(Long companyId);
    Optional<GlobalInvitation> findByCompanyIdAndActiveTrue(Long companyId);
}
