package com.viandas.api.delivery.persistence;

import java.util.UUID;

import com.viandas.api.delivery.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryLocationUpdateRepository extends JpaRepository<DeliveryLocationUpdate, UUID> {
}
