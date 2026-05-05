package com.viandas.api.company.persistence;

import java.util.UUID;

import com.viandas.api.company.domain.*;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyMembershipRepository extends JpaRepository<CompanyMembership, UUID> {
	Optional<CompanyMembership> findFirstByUserIdOrderById(UUID userId);

	Optional<CompanyMembership> findByUserId(UUID userId);

	long countByUserId(UUID userId);

	boolean existsByCompanyIdAndUserId(UUID companyId, UUID userId);

	boolean existsByUserId(UUID userId);
}
