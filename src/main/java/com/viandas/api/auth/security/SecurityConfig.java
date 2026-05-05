package com.viandas.api.auth.security;

import java.time.Instant;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.viandas.api.shared.ApiError;
import com.viandas.api.shared.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			JwtAuthenticationFilter jwtAuthenticationFilter,
			ObjectMapper objectMapper
	) throws Exception {
		return http
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint((request, response, exception) ->
								writeError(objectMapper, request, response, HttpStatus.UNAUTHORIZED, "Authentication required"))
						.accessDeniedHandler((request, response, exception) ->
								writeError(objectMapper, request, response, HttpStatus.FORBIDDEN, "Access denied")))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/auth/**", "/internal/bootstrap/cook", "/invitations/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/public/menus/*/*").permitAll()
						.requestMatchers(HttpMethod.GET, "/global-invitation/*").permitAll()
						.requestMatchers(HttpMethod.POST, "/global-invitation/*/accept").permitAll()
						.anyRequest().authenticated())
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
				.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

	private static void writeError(
			ObjectMapper objectMapper,
			HttpServletRequest request,
			HttpServletResponse response,
			HttpStatus status,
			String message
	) throws java.io.IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getWriter(), ApiResponse.error(
				message,
				List.of(new ApiError(message)),
				meta(status, request)));
	}

	private static Map<String, Object> meta(HttpStatus status, HttpServletRequest request) {
		Map<String, Object> meta = new LinkedHashMap<>();
		meta.put("status", status.value());
		meta.put("path", request.getRequestURI());
		meta.put("timestamp", Instant.now());
		return meta;
	}
}
