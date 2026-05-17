package com.viandas.api.notification.config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "viandas.firebase.credentials-json", matchIfMissing = false)
public class FirebaseConfig {

	private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

	@Bean
	FirebaseMessaging firebaseMessaging(@Value("${viandas.firebase.credentials-json}") String credentialsJson) throws IOException {
		if (credentialsJson == null || credentialsJson.isBlank()) {
			throw new IllegalStateException("viandas.firebase.credentials-json is empty");
		}
		try (ByteArrayInputStream in = new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8))) {
			GoogleCredentials credentials = GoogleCredentials.fromStream(in);
			FirebaseOptions options = FirebaseOptions.builder()
					.setCredentials(credentials)
					.build();
			FirebaseApp app = FirebaseApp.getApps().stream()
					.filter(a -> "viandas".equals(a.getName()))
					.findFirst()
					.orElseGet(() -> FirebaseApp.initializeApp(options, "viandas"));
			log.info("Firebase initialized for app '{}'", app.getName());
			return FirebaseMessaging.getInstance(app);
		}
	}
}
