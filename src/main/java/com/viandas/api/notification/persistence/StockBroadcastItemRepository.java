package com.viandas.api.notification.persistence;

import java.util.UUID;

import com.viandas.api.notification.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockBroadcastItemRepository extends JpaRepository<StockBroadcastItem, UUID> {

    @Modifying
    @Query("DELETE FROM StockBroadcastItem i WHERE i.stockBroadcast.company.id = :companyId")
    void deleteByStockBroadcastCompanyId(@Param("companyId") UUID companyId);
}
