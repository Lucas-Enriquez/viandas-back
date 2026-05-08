package com.viandas.api.company.persistence;

import java.util.UUID;

import com.viandas.api.company.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanyLocationRepository extends JpaRepository<CompanyLocation, UUID> {

    @Modifying
    @Query("DELETE FROM CompanyLocation l WHERE l.company.id = :companyId")
    void deleteByCompanyId(@Param("companyId") UUID companyId);
}
