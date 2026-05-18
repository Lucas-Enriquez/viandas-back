package com.viandas.api.company.persistence;

import java.util.UUID;

import com.viandas.api.company.domain.*;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanyMembershipRepository extends JpaRepository<CompanyMembership, UUID> {
	Optional<CompanyMembership> findFirstByUserIdOrderById(UUID userId);

	Optional<CompanyMembership> findByUserId(UUID userId);

	long countByUserId(UUID userId);

	boolean existsByCompanyIdAndUserId(UUID companyId, UUID userId);

	boolean existsByUserId(UUID userId);

	@Query("""
			select m from CompanyMembership m
			join fetch m.user u
			where m.company.id in :companyIds
			and u.role = com.viandas.api.user.domain.UserRole.EMPLOYEE
			""")
	List<CompanyMembership> findEmployeesByCompanyIdIn(@Param("companyIds") Collection<UUID> companyIds);
}
