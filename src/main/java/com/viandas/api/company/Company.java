package com.viandas.api.company;

import java.math.BigDecimal;
import java.time.Instant;

import com.viandas.api.user.User;

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
@Table(name = "companies")
public class Company {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "cook_id", nullable = false)
	private User cook;

	@Column(nullable = false, length = 160)
	private String name;

	@Column(nullable = false, unique = true, length = 180)
	private String slug;

	private String address;

	@Column(columnDefinition = "TEXT")
	private String notes;

	@Column(precision = 10, scale = 7)
	private BigDecimal latitude;

	@Column(precision = 10, scale = 7)
	private BigDecimal longitude;

	@Enumerated(EnumType.STRING)
	@Column(name = "location_source", length = 40)
	private LocationSource locationSource;

	@Column(name = "whatsapp_group_label", length = 160)
	private String whatsappGroupLabel;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt = Instant.now();

	public Company(User cook, String name, String slug) {
		this.cook = cook;
		this.name = name;
		this.slug = slug;
	}
}
