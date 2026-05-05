package com.viandas.api.order.persistence;

import java.util.UUID;

import com.viandas.api.order.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {
}
