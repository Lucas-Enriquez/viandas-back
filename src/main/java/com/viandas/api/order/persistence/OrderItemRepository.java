package com.viandas.api.order.persistence;

import com.viandas.api.order.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
