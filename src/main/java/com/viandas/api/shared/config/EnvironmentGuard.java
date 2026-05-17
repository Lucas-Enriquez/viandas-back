package com.viandas.api.shared.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Falla al arranque si en un perfil distinto de "dev" alguna variable obligatoria
 * esta vacia o conserva un valor default de desarrollo. Evita prender produccion
 * con secretos por defecto.
 */
@Component
@Profile("!dev")
public class EnvironmentGuard implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(EnvironmentGuard.class);

	private static final List<String> REQUIRED_PROPERTIES = List.of(
			"spring.datasource.password",
			"viandas.google.client-id",
			"viandas.cloudinary.cloud-name",
			"viandas.cloudinary.api-key",
			"viandas.cloudinary.api-secret",
			"viandas.firebase.credentials-json",
			"viandas.resend.api-key",
			"viandas.resend.from",
			"sentry.dsn");

	private static final Set<String> FORBIDDEN_JWT_SECRETS = Set.of(
			"dev-secret-change-me-dev-secret-change-me");

	private static final Set<String> FORBIDDEN_BOOTSTRAP_KEYS = Set.of(
			"dev-bootstrap-key");

	private final Environment environment;

	public EnvironmentGuard(Environment environment) {
		this.environment = environment;
	}

	@Override
	public void run(ApplicationArguments args) {
		List<String> errors = new ArrayList<>();

		for (String property : REQUIRED_PROPERTIES) {
			String value = environment.getProperty(property);
			if (value == null || value.isBlank()) {
				errors.add("Variable obligatoria vacia: " + property);
			}
		}

		String jwtSecret = environment.getProperty("viandas.jwt.secret");
		if (jwtSecret == null || jwtSecret.isBlank()) {
			errors.add("viandas.jwt.secret esta vacio");
		} else if (FORBIDDEN_JWT_SECRETS.contains(jwtSecret)) {
			errors.add("viandas.jwt.secret tiene el valor default de desarrollo");
		} else if (jwtSecret.length() < 32) {
			errors.add("viandas.jwt.secret debe tener al menos 32 caracteres");
		}

		String bootstrapKey = environment.getProperty("viandas.bootstrap.key");
		if (bootstrapKey != null && FORBIDDEN_BOOTSTRAP_KEYS.contains(bootstrapKey)) {
			errors.add("viandas.bootstrap.key tiene el valor default de desarrollo");
		}

		String corsOrigins = environment.getProperty("viandas.cors.allowed-origins");
		if (corsOrigins != null && corsOrigins.contains("localhost")) {
			errors.add("viandas.cors.allowed-origins incluye localhost en un perfil no-dev");
		}

		if (!errors.isEmpty()) {
			String message = "Configuracion insegura para entornos no-dev:\n  - " + String.join("\n  - ", errors);
			log.error(message);
			throw new IllegalStateException(message);
		}

		log.info("EnvironmentGuard OK: todas las variables criticas estan configuradas");
	}
}
