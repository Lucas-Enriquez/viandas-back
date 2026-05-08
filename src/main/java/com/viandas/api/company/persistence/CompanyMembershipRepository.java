package com.viandas.api.company.persistence;

import java.util.UUID;

import com.viandas.api.company.domain.*;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanyMembershipRepository extends JpaRepository<CompanyMembership, UUID> {
	Optional<CompanyMembership> findFirstByUserIdOrderById(UUID userId);

	Optional<CompanyMembership> findByUserId(UUID userId);

	long countByUserId(UUID userId);

	boolean existsByCompanyIdAndUserId(UUID companyId, UUID userId);

	boolean existsByUserId(UUID userId);

    @Modifying
    @Query("DELETE FROM CompanyMembership m WHERE m.company.id = :companyId")
    void deleteByCompanyId(@Param("companyId") UUID companyId);
}
