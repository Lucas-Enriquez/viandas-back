package com.viandas.api.auth.application;

import java.time.Clock;
import java.time.Instant;

import com.viandas.api.auth.dto.request.ForgotPasswordRequest;
import com.viandas.api.auth.dto.request.ResetPasswordRequest;
import com.viandas.api.auth.token.PasswordResetToken;
import com.viandas.api.auth.token.PasswordResetTokenRepository;
import com.viandas.api.auth.token.RefreshTokenRepository;
import com.viandas.api.shared.ApiException;
import com.viandas.api.shared.TokenHasher;
import com.viandas.api.shared.email.EmailService;
import com.viandas.api.user.domain.User;
import com.viandas.api.user.persistence.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetService {

	private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

	private final UserRepository userRepository;
	private final PasswordResetTokenRepository passwordResetTokenRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final TokenHasher tokenHasher;
	private final PasswordEncoder passwordEncoder;
	private final EmailService emailService;
	private final Clock clock;
	private final long expirationMinutes;

	public PasswordResetService(
			UserRepository userRepository,
			PasswordResetTokenRepository passwordResetTokenRepository,
			RefreshTokenRepository refreshTokenRepository,
			TokenHasher tokenHasher,
			PasswordEncoder passwordEncoder,
			EmailService emailService,
			Clock clock,
			@Value("${viandas.password-reset.expiration-minutes:30}") long expirationMinutes) {
		this.userRepository = userRepository;
		this.passwordResetTokenRepository = passwordResetTokenRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.tokenHasher = tokenHasher;
		this.passwordEncoder = passwordEncoder;
		this.emailService = emailService;
		this.clock = clock;
		this.expirationMinutes = expirationMinutes;
	}

	@Transactional
	public void requestReset(ForgotPasswordRequest request) {
		String email = request.email().trim().toLowerCase();
		User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
		if (user == null || !user.isEnabled() || user.getPasswordHash() == null) {
			log.info("Password reset requested for non-resettable email (silent): {}", email);
			return;
		}
		passwordResetTokenRepository.deleteByUserId(user.getId());
		String rawToken = tokenHasher.newToken();
		PasswordResetToken token = new PasswordResetToken(
				user,
				tokenHasher.hash(rawToken),
				Instant.now(clock).plusSeconds(expirationMinutes * 60));
		passwordResetTokenRepository.save(token);
		emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), rawToken);
	}

	@Transactional
	public void confirmReset(ResetPasswordRequest request) {
		String tokenHash = tokenHasher.hash(request.token());
		PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(tokenHash)
				.orElseThrow(() -> ApiException.badRequest("Token invalido o expirado"));
		Instant now = Instant.now(clock);
		if (!token.isUsable(now)) {
			throw ApiException.badRequest("Token invalido o expirado");
		}
		User user = token.getUser();
		if (!user.isEnabled()) {
			throw ApiException.badRequest("Token invalido o expirado");
		}
		user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
		user.setUpdatedAt(now);
		token.setUsedAt(now);
		int revoked = refreshTokenRepository.revokeAllActiveByUserId(user.getId(), now);
		log.info("Password reset confirmed for user {}: revoked {} refresh tokens", user.getId(), revoked);
	}
}
