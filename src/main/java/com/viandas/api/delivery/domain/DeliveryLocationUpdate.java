package com.viandas.api.delivery.domain;

import java.math.BigDecimal;
import java.time.Instant;

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
@Table(name = "delivery_location_updates")
public class DeliveryLocationUpdate {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "delivery_session_id", nullable = false)
	private DeliverySession deliverySession;

	@Column(name = "approx_latitude", nullable = false, precision = 10, scale = 7)
	private BigDecimal approxLatitude;

	@Column(name = "approx_longitude", nullable = false, precision = 10, scale = 7)
	private BigDecimal approxLongitude;

	@Column(name = "accuracy_meters", precision = 8, scale = 2)
	private BigDecimal accuracyMeters;

	@Enumerated(EnumType.STRING)
	@Column(name = "public_signal", nullable = false, length = 40)
	private DeliveryPublicSignal publicSignal;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();
}
