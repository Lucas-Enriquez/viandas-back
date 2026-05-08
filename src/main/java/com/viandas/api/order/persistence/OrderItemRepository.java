package com.viandas.api.order.persistence;

import java.util.UUID;

import com.viandas.api.order.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    @Modifying
    @Query("DELETE FROM OrderItem i WHERE i.order.company.id = :companyId")
    void deleteByOrderCompanyId(@Param("companyId") UUID companyId);
}
