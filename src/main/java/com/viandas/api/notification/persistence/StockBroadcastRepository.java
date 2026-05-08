package com.viandas.api.notification.persistence;

import java.util.UUID;

import com.viandas.api.notification.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockBroadcastRepository extends JpaRepository<StockBroadcast, UUID> {

    @Modifying
    @Query("DELETE FROM StockBroadcast s WHERE s.company.id = :companyId")
    void deleteByCompanyId(@Param("companyId") UUID companyId);
}
