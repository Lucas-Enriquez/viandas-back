package com.viandas.api.delivery.domain;

import java.math.BigDecimal;
import java.time.Instant;

import com.viandas.api.company.domain.Company;
import com.viandas.api.menu.domain.Menu;
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
@Table(name = "delivery_sessions")
public class DeliverySession {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "company_id", nullable = false)
	private Company company;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "menu_id", nullable = false)
	private Menu menu;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "cook_id", nullable = false)
	private User cook;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private DeliverySessionStatus status = DeliverySessionStatus.ACTIVE;

	@Column(name = "started_at", nullable = false)
	private Instant startedAt;

	@Column(name = "finished_at")
	private Instant finishedAt;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "last_approx_latitude", precision = 10, scale = 7)
	private BigDecimal lastApproxLatitude;

	@Column(name = "last_approx_longitude", precision = 10, scale = 7)
	private BigDecimal lastApproxLongitude;

	@Column(name = "last_location_at")
	private Instant lastLocationAt;
}
