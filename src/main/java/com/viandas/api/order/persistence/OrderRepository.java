package com.viandas.api.order.persistence;

import java.util.UUID;

import com.viandas.api.order.domain.*;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<CustomerOrder, UUID> {
	@EntityGraph(attributePaths = {"items", "items.menuItem", "menu", "company"})
	List<CustomerOrder> findByCompanyCookIdAndCreatedAtBetweenOrderByCreatedAtDesc(UUID cookId, Instant from, Instant to);

	@EntityGraph(attributePaths = {"items", "items.menuItem", "menu", "company"})
	Optional<CustomerOrder> findFirstByMenuIdAndCustomerIdAndStatusNotOrderByCreatedAtDesc(UUID menuId, UUID customerId, OrderStatus status);

	@EntityGraph(attributePaths = {"items", "items.menuItem", "menu", "company"})
	Optional<CustomerOrder> findFirstByMenuIdAndEmployeeIdAndStatusNotOrderByCreatedAtDesc(UUID menuId, UUID employeeId, OrderStatus status);

	Optional<CustomerOrder> findByIdAndCompanyCookId(UUID id, UUID cookId);

	List<CustomerOrder> findByMenuIdAndCompanyIdAndStatusIn(UUID menuId, UUID companyId, List<OrderStatus> statuses);

	CustomerOrder existsByMenuId(UUID id);
}
