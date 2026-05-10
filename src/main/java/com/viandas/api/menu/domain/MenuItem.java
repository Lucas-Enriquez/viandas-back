package com.viandas.api.menu.domain;

import java.util.UUID;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

import com.viandas.api.company.domain.Company;
import com.viandas.api.product.domain.Product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
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
@Table(name = "menu_items")
public class MenuItem {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "menu_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Menu menu;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id")
	@OnDelete(action = OnDeleteAction.SET_NULL)
	private Product product;

	@Column(nullable = false, length = 180)
	private String name;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal price;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private MenuItemCategory category;

	@Column(name = "photo_url", length = 500)
	private String photoUrl;

	@Column(name = "remaining_stock")
	private Integer remainingStock;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	@ManyToMany
	@JoinTable(
			name = "menu_item_companies",
			joinColumns = @JoinColumn(name = "menu_item_id"),
			inverseJoinColumns = @JoinColumn(name = "company_id"))
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Set<Company> availableCompanies = new LinkedHashSet<>();

	public MenuItem(Menu menu, String name, BigDecimal price, MenuItemCategory category) {
		this.menu = menu;
		this.name = name;
		this.price = price;
		this.category = category;
	}
}
