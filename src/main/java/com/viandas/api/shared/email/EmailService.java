package com.viandas.api.shared.email;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import tools.jackson.databind.ObjectMapper;

@Service
public class EmailService {

	private static final Logger log = LoggerFactory.getLogger(EmailService.class);
	private static final URI RESEND_ENDPOINT = URI.create("https://api.resend.com/emails");

	private final ObjectMapper objectMapper;
	private final String apiKey;
	private final String from;
	private final String publicBaseUrl;
	private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

	public EmailService(
			ObjectMapper objectMapper,
			@Value("${viandas.resend.api-key:}") String apiKey,
			@Value("${viandas.resend.from:}") String from,
			@Value("${viandas.public-base-url}") String publicBaseUrl) {
		this.objectMapper = objectMapper;
		this.apiKey = apiKey == null ? "" : apiKey.trim();
		this.from = from == null ? "" : from.trim();
		this.publicBaseUrl = publicBaseUrl;
	}

	@Async
	public void sendPasswordResetEmail(String to, String name, String token) {
		String resetUrl = publicBaseUrl + "/reset-password?token=" + token;
		String safeName = name == null || name.isBlank() ? "" : escapeHtml(name);
		String html = """
				<div style="font-family:system-ui,sans-serif;max-width:520px;margin:0 auto;padding:24px;">
				  <h2>Recuperacion de contrasena</h2>
				  <p>Hola %s,</p>
				  <p>Recibimos una solicitud para restablecer tu contrasena. Hace click en el siguiente boton para crear una nueva:</p>
				  <p style="margin:24px 0;">
				    <a href="%s" style="display:inline-block;padding:12px 24px;background:#0a7;color:#fff;text-decoration:none;border-radius:6px;">Restablecer contrasena</a>
				  </p>
				  <p>O copia este enlace en tu navegador:<br><a href="%s">%s</a></p>
				  <p style="color:#666;font-size:13px;">El enlace expira en 30 minutos. Si no solicitaste esto, ignora este correo.</p>
				</div>
				""".formatted(safeName, resetUrl, resetUrl, resetUrl);

		send(to, "Recuperacion de contrasena - Viandas", html);
	}

	private void send(String to, String subject, String html) {
		if (apiKey.isBlank() || from.isBlank()) {
			log.info("Email noop (Resend not configured): to={} subject='{}'", to, subject);
			return;
		}
		try {
			String body = objectMapper.writeValueAsString(Map.of(
					"from", from,
					"to", List.of(to),
					"subject", subject,
					"html", html));
			HttpRequest request = HttpRequest.newBuilder()
					.uri(RESEND_ENDPOINT)
					.timeout(Duration.ofSeconds(10))
					.header("Authorization", "Bearer " + apiKey)
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
					.build();
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() >= 200 && response.statusCode() < 300) {
				log.info("Email sent to {} subject='{}'", to, subject);
			} else {
				log.warn("Email send failed: status={} body={}", response.statusCode(), response.body());
			}
		} catch (Exception exception) {
			log.error("Email send error to {} subject='{}'", to, subject, exception);
		}
	}

	private static String escapeHtml(String input) {
		return input
				.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;")
				.replace("\"", "&quot;")
				.replace("'", "&#39;");
	}
}
