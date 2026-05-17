package com.viandas.api.auth.token;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
	Optional<PasswordResetToken> findByTokenHash(String tokenHash);

	@Modifying
	@Transactional
	void deleteByUserId(UUID userId);
}
