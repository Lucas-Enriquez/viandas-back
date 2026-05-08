package com.viandas.api.invitation.persistence;

import com.viandas.api.invitation.domain.*;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvitationRepository extends JpaRepository<Invitation, UUID> {
	Optional<Invitation> findByTokenHash(String tokenHash);

    @Modifying
    @Query("DELETE FROM Invitation i WHERE i.company.id = :companyId")
    void deleteByCompanyId(@Param("companyId") UUID companyId);
}
