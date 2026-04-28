package com.viandas.api.company;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyMembershipRepository extends JpaRepository<CompanyMembership, Long> {
	Optional<CompanyMembership> findFirstByUserIdOrderById(Long userId);

	boolean existsByCompanyIdAndUserId(Long companyId, Long userId);
}
