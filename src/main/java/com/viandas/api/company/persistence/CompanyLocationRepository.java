package com.viandas.api.company.persistence;

import com.viandas.api.company.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyLocationRepository extends JpaRepository<CompanyLocation, Long> {
}
