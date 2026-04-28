package com.viandas.api.order;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {
	@EntityGraph(attributePaths = {"items", "items.menuItem", "menu", "company"})
	List<CustomerOrder> findByCompanyCookIdAndCreatedAtBetweenOrderByCreatedAtDesc(Long cookId, Instant from, Instant to);

	@EntityGraph(attributePaths = {"items", "items.menuItem", "menu", "company"})
	Optional<CustomerOrder> findFirstByMenuIdAndCustomerIdAndStatusNotOrderByCreatedAtDesc(Long menuId, Long customerId, OrderStatus status);

	Optional<CustomerOrder> findByIdAndCompanyCookId(Long id, Long cookId);

	List<CustomerOrder> findByMenuIdAndCompanyIdAndStatusIn(Long menuId, Long companyId, List<OrderStatus> statuses);
}
