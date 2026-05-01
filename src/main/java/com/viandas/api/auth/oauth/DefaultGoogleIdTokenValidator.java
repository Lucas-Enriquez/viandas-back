package com.viandas.api.auth.oauth;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.viandas.api.shared.ApiException;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Component
public class DefaultGoogleIdTokenValidator implements GoogleIdTokenValidator {
	private final ObjectMapper objectMapper;
	private final String clientId;
	private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

	public DefaultGoogleIdTokenValidator(ObjectMapper objectMapper, @Value("${viandas.google.client-id:}") String clientId) {
		this.objectMapper = objectMapper;
		this.clientId = clientId == null ? "" : clientId.trim();
	}

	@Override
	public GoogleProfile validate(String idToken) {
		if (idToken == null || idToken.isBlank()) {
			throw ApiException.badRequest("idToken is required");
		}
		if (clientId.isBlank()) {
			return validateDevToken(idToken);
		}
		return validateWithGoogle(idToken);
	}

	private GoogleProfile validateDevToken(String idToken) {
		String[] parts = idToken.split(":", 3);
		if (parts.length >= 2 && parts[0].equals("dev")) {
			String email = parts[1].trim().toLowerCase();
			String name = parts.length == 3 && !parts[2].isBlank() ? parts[2].trim() : email;
			return new GoogleProfile("dev-" + email, email, name);
		}
		if (idToken.contains("@")) {
			String email = idToken.trim().toLowerCase();
			return new GoogleProfile("dev-" + email, email, email);
		}
		throw ApiException.unauthorized("Invalid Google token");
	}

	private GoogleProfile validateWithGoogle(String idToken) {
		try {
			String encodedToken = URLEncoder.encode(idToken, StandardCharsets.UTF_8);
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create("https://oauth2.googleapis.com/tokeninfo?id_token=" + encodedToken))
					.timeout(Duration.ofSeconds(5))
					.GET()
					.build();
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				throw ApiException.unauthorized("Invalid Google token");
			}
			Map<String, Object> payload = objectMapper.readValue(response.body(), new TypeReference<>() {
			});
			if (!clientId.equals(payload.get("aud"))) {
				throw ApiException.unauthorized("Invalid Google audience");
			}
			if (!"true".equals(String.valueOf(payload.get("email_verified")))) {
				throw ApiException.unauthorized("Google email is not verified");
			}
			String subject = String.valueOf(payload.get("sub"));
			String email = String.valueOf(payload.get("email")).toLowerCase();
			String name = String.valueOf(payload.getOrDefault("name", email));
			return new GoogleProfile(subject, email, name);
		} catch (ApiException exception) {
			throw exception;
		} catch (Exception exception) {
			throw ApiException.unauthorized("Could not validate Google token");
		}
	}
}
