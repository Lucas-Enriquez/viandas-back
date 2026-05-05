package com.viandas.api.menu.domain;

import java.util.UUID;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.viandas.api.company.domain.Company;
import com.viandas.api.user.domain.User;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "menus")
public class Menu {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "cook_id", nullable = false)
	private User cook;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "company_id")
	private Company company;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private MenuScope scope = MenuScope.COMPANY;

	@Column(name = "menu_date", nullable = false)
	private LocalDate menuDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private MenuStatus status = MenuStatus.DRAFT;

	@Column(name = "order_closes_at", nullable = false)
	private LocalTime orderClosesAt;

	@Column(name = "published_at")
	private Instant publishedAt;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt = Instant.now();

	@OneToMany(mappedBy = "menu", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<MenuItem> items = new ArrayList<>();

	@ManyToMany
	@JoinTable(
			name = "menu_companies",
			joinColumns = @JoinColumn(name = "menu_id"),
			inverseJoinColumns = @JoinColumn(name = "company_id"))
	private Set<Company> assignedCompanies = new LinkedHashSet<>();

	public Menu(Company company, LocalDate menuDate, LocalTime orderClosesAt) {
		this.cook = company.getCook();
		this.company = company;
		this.scope = MenuScope.COMPANY;
		this.menuDate = menuDate;
		this.orderClosesAt = orderClosesAt;
	}

	public Menu(User cook, LocalDate menuDate, LocalTime orderClosesAt) {
		this.cook = cook;
		this.scope = MenuScope.GLOBAL;
		this.menuDate = menuDate;
		this.orderClosesAt = orderClosesAt;
	}
}
