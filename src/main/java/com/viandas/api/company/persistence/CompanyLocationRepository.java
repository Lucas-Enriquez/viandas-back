package com.viandas.api.company.persistence;

import java.util.UUID;

import com.viandas.api.company.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyLocationRepository extends JpaRepository<CompanyLocation, UUID> {
}
