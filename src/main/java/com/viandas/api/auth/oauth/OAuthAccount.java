package com.viandas.api.auth.oauth;

import java.time.Instant;

import com.viandas.api.user.domain.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "oauth_accounts")
public class OAuthAccount {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private OAuthProvider provider;

	@Column(name = "provider_subject", nullable = false)
	private String providerSubject;

	@Column(name = "provider_email", nullable = false)
	private String providerEmail;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt = Instant.now();

	public OAuthAccount(User user, OAuthProvider provider, String providerSubject, String providerEmail) {
		this.user = user;
		this.provider = provider;
		this.providerSubject = providerSubject;
		this.providerEmail = providerEmail;
	}
}
