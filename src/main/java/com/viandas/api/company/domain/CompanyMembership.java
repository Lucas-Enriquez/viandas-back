package com.viandas.api.company.domain;

import java.util.UUID;

import java.time.Instant;

import com.viandas.api.user.domain.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
		name = "company_memberships",
		uniqueConstraints = @UniqueConstraint(name = "uk_company_memberships_user", columnNames = "user_id"))
public class CompanyMembership {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "company_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Company company;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	public CompanyMembership(Company company, User user) {
		this.company = company;
		this.user = user;
	}
}
