package com.viandas.api.invitation.persistence;

import com.viandas.api.invitation.domain.*;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InvitationRepository extends JpaRepository<Invitation, UUID> {
	Optional<Invitation> findByTokenHash(String tokenHash);
}
