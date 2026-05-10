package com.viandas.api.company.domain;

import java.util.UUID;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "company_locations")
public class CompanyLocation {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "company_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Company company;

	private String address;

	@Column(precision = 10, scale = 7)
	private BigDecimal latitude;

	@Column(precision = 10, scale = 7)
	private BigDecimal longitude;

	@Enumerated(EnumType.STRING)
	@Column(name = "location_source", length = 40)
	private LocationSource locationSource;

	@Column(name = "changed_at", nullable = false)
	private Instant changedAt = Instant.now();

	public CompanyLocation(Company company) {
		this.company = company;
		this.address = company.getAddress();
		this.latitude = company.getLatitude();
		this.longitude = company.getLongitude();
		this.locationSource = company.getLocationSource();
	}
}
