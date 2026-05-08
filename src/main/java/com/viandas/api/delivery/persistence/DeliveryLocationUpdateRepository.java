package com.viandas.api.delivery.persistence;

import java.util.UUID;

import com.viandas.api.delivery.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeliveryLocationUpdateRepository extends JpaRepository<DeliveryLocationUpdate, UUID> {

    @Modifying
    @Query("DELETE FROM DeliveryLocationUpdate d WHERE d.deliverySession.company.id = :companyId")
    void deleteByDeliverySessionCompanyId(@Param("companyId") UUID companyId);
}
