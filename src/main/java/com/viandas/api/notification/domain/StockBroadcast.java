package com.viandas.api.notification.domain;

import java.util.UUID;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.viandas.api.company.domain.Company;
import com.viandas.api.menu.domain.Menu;
import com.viandas.api.user.domain.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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
@Table(name = "stock_broadcasts")
public class StockBroadcast {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "company_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Company company;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "menu_id", nullable = false)
	private Menu menu;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "sent_by", nullable = false)
	private User sentBy;

	@Column(length = 500)
	private String message;

	@Column(name = "sent_at", nullable = false)
	private Instant sentAt = Instant.now();

	@OneToMany(mappedBy = "stockBroadcast", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<StockBroadcastItem> items = new ArrayList<>();
}
