package com.viandas.api.product.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.viandas.api.menu.domain.MenuItemCategory;
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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "products", uniqueConstraints = @UniqueConstraint(name = "uk_products_cook_name", columnNames = {"cook_id", "name"}))
public class Product {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "cook_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private User cook;

	@Column(nullable = false, length = 180)
	private String name;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal price;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private MenuItemCategory category;

	@Column(name = "photo_url", length = 500)
	private String photoUrl;

	@Column(name = "photo_public_id", length = 255)
	private String photoPublicId;

	@Column(length = 500)
	private String description;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt = Instant.now();

	public Product(User cook, String name, BigDecimal price, MenuItemCategory category) {
		this.cook = cook;
		this.name = name;
		this.price = price;
		this.category = category;
	}
}
