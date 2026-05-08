package com.viandas.api.invitation.persistence;

import com.viandas.api.invitation.domain.GlobalInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface GlobalInvitationRepository extends JpaRepository<GlobalInvitation, UUID> {

    Optional<GlobalInvitation> findByTokenHash(String tokenHash);
    Optional<GlobalInvitation> findByTokenHashAndActiveTrue(String tokenHash);
    Optional<GlobalInvitation> findByCompanyId(UUID companyId);
    Optional<GlobalInvitation> findByCompanyIdAndActiveTrue(UUID companyId);

    @Modifying
    @Query("DELETE FROM GlobalInvitation g WHERE g.company.id = :companyId")
    void deleteByCompanyId(@Param("companyId") UUID companyId);
}
