package com.viandas.api.invitation;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InvitationRepository extends JpaRepository<Invitation, UUID> {
	Optional<Invitation> findByToken(UUID token);
}
