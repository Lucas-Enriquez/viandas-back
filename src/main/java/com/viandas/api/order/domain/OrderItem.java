package com.viandas.api.order.domain;

import java.util.UUID;

import java.math.BigDecimal;

import com.viandas.api.menu.domain.MenuItem;

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
@Table(name = "order_items")
public class OrderItem {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private CustomerOrder order;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "menu_item_id", nullable = false)
	private MenuItem menuItem;

	@Column(name = "item_name_snapshot", nullable = false, length = 180)
	private String itemNameSnapshot;

	@Column(name = "unit_price_snapshot", nullable = false, precision = 12, scale = 2)
	private BigDecimal unitPriceSnapshot;

	@Column(nullable = false)
	private int quantity;

	@Column(length = 500)
	private String comment;
}
