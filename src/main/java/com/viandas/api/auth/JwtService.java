package com.viandas.api.auth;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.viandas.api.shared.ApiException;
import com.viandas.api.user.UserRole;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
public class JwtService {
	private static final String HMAC_ALGORITHM = "HmacSHA256";
	private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
	private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

	private final ObjectMapper objectMapper;
	private final Clock clock;
	private final String secret;
	private final long expirationMinutes;

	public JwtService(
			ObjectMapper objectMapper,
			Clock clock,
			@Value("${viandas.jwt.secret}") String secret,
			@Value("${viandas.jwt.expiration-minutes}") long expirationMinutes) {
		this.objectMapper = objectMapper;
		this.clock = clock;
		this.secret = secret;
		this.expirationMinutes = expirationMinutes;
	}

	public String createToken(Long userId, String email, UserRole role, Long companyId) {
		try {
			Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
			Map<String, Object> payload = new LinkedHashMap<>();
			payload.put("sub", email);
			payload.put("userId", userId);
			payload.put("role", role.name());
			if (companyId != null) {
				payload.put("companyId", companyId);
			}
			payload.put("iat", Instant.now(clock).getEpochSecond());
			payload.put("exp", Instant.now(clock).plusSeconds(expirationMinutes * 60).getEpochSecond());

			String encodedHeader = encodeJson(header);
			String encodedPayload = encodeJson(payload);
			String signingInput = encodedHeader + "." + encodedPayload;
			return signingInput + "." + sign(signingInput);
		} catch (Exception exception) {
			throw new IllegalStateException("Could not create JWT", exception);
		}
	}

	public CurrentUser parse(String token) {
		try {
			String[] parts = token.split("\\.");
			if (parts.length != 3) {
				throw ApiException.unauthorized("Invalid token");
			}
			String signingInput = parts[0] + "." + parts[1];
			if (!constantTimeEquals(sign(signingInput), parts[2])) {
				throw ApiException.unauthorized("Invalid token signature");
			}
			Map<String, Object> payload = objectMapper.readValue(URL_DECODER.decode(parts[1]), new TypeReference<>() {
			});
			long exp = ((Number) payload.get("exp")).longValue();
			if (Instant.now(clock).getEpochSecond() >= exp) {
				throw ApiException.unauthorized("Expired token");
			}
			Long userId = ((Number) payload.get("userId")).longValue();
			UserRole role = UserRole.valueOf((String) payload.get("role"));
			Object company = payload.get("companyId");
			Long companyId = company == null ? null : ((Number) company).longValue();
			return new CurrentUser(userId, role, companyId);
		} catch (ApiException exception) {
			throw exception;
		} catch (Exception exception) {
			throw ApiException.unauthorized("Invalid token");
		}
	}

	private String encodeJson(Object value) throws Exception {
		return URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
	}

	private String sign(String signingInput) throws Exception {
		Mac mac = Mac.getInstance(HMAC_ALGORITHM);
		mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
		return URL_ENCODER.encodeToString(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
	}

	private static boolean constantTimeEquals(String left, String right) {
		byte[] leftBytes = left.getBytes(StandardCharsets.UTF_8);
		byte[] rightBytes = right.getBytes(StandardCharsets.UTF_8);
		if (leftBytes.length != rightBytes.length) {
			return false;
		}
		int result = 0;
		for (int i = 0; i < leftBytes.length; i++) {
			result |= leftBytes[i] ^ rightBytes[i];
		}
		return result == 0;
	}
}
