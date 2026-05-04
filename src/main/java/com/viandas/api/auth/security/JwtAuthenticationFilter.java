package com.viandas.api.auth.security;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.viandas.api.shared.ApiError;
import com.viandas.api.shared.ApiException;
import com.viandas.api.shared.ApiResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
	private final JwtService jwtService;
	private final ObjectMapper objectMapper;

	public JwtAuthenticationFilter(JwtService jwtService, ObjectMapper objectMapper) {
		this.jwtService = jwtService;
		this.objectMapper = objectMapper;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header != null && header.startsWith("Bearer ")) {
			try {
				CurrentUser currentUser = jwtService.parse(header.substring(7));
				var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + currentUser.role().name()));
				var authentication = new UsernamePasswordAuthenticationToken(currentUser, null, authorities);
				SecurityContextHolder.getContext().setAuthentication(authentication);
			} catch (ApiException exception) {
				response.setStatus(HttpStatus.UNAUTHORIZED.value());
				response.setContentType("application/json");
				objectMapper.writeValue(response.getWriter(), ApiResponse.error(
						exception.getMessage(),
						List.of(new ApiError(exception.getMessage())),
						meta(request)));
				return;
			}
		}
		filterChain.doFilter(request, response);
	}

	private static Map<String, Object> meta(HttpServletRequest request) {
		Map<String, Object> meta = new LinkedHashMap<>();
		meta.put("status", HttpStatus.UNAUTHORIZED.value());
		meta.put("path", request.getRequestURI());
		meta.put("timestamp", Instant.now());
		return meta;
	}
}
