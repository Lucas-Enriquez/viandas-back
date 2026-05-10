package com.viandas.api.menu.domain;

import java.util.UUID;

import java.time.Instant;

import com.viandas.api.company.domain.Company;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "menu_public_links")
public class MenuPublicLink {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "menu_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Menu menu;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "company_id")
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Company company;

	@Column(name = "token_hash", nullable = false, unique = true, length = 128)
	private String tokenHash;

	@Column(nullable = false)
	private boolean active = true;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	public MenuPublicLink(Menu menu, Company company, String tokenHash, Instant expiresAt) {
		this.menu = menu;
		this.company = company;
		this.tokenHash = tokenHash;
		this.expiresAt = expiresAt;
	}
}
