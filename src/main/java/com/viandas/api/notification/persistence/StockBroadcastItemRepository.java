package com.viandas.api.notification.persistence;

import com.viandas.api.notification.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockBroadcastItemRepository extends JpaRepository<StockBroadcastItem, Long> {
}
