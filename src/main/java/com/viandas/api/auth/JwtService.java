package com.viandas.api.auth;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.viandas.api.shared.ApiException;
import com.viandas.api.user.UserRole;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {
	private final Clock clock;
	private final SecretKey signingKey;
	private final long expirationMinutes;

	public JwtService(
			Clock clock,
			@Value("${viandas.jwt.secret}") String secret,
			@Value("${viandas.jwt.expiration-minutes}") long expirationMinutes) {
		this.clock = clock;
		this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.expirationMinutes = expirationMinutes;
	}

	public String createToken(Long userId, String email, UserRole role) {
		Instant now = Instant.now(clock);
		Instant expiresAt = now.plusSeconds(expirationMinutes * 60);

		var builder = Jwts.builder()
				.subject(email)
				.claim("userId", userId)
				.claim("role", role.name())
				.issuedAt(Date.from(now))
				.expiration(Date.from(expiresAt))
				.signWith(signingKey);

		return builder.compact();
	}

	public CurrentUser parse(String token) {
		try {
			Claims claims = Jwts.parser()
					.verifyWith(signingKey)
					.clock(() -> Date.from(Instant.now(clock)))
					.build()
					.parseSignedClaims(token)
					.getPayload();

			Long userId = claims.get("userId", Long.class);
			UserRole role = UserRole.valueOf(claims.get("role", String.class));
			Long companyId = claims.get("companyId", Long.class);

			return new CurrentUser(userId, role, companyId);
		} catch (JwtException | IllegalArgumentException exception) {
			throw ApiException.unauthorized("Invalid token");
		}
	}
}
