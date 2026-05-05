package com.viandas.api.user.application;

import com.viandas.api.auth.dto.response.AuthUserResponse;
import com.viandas.api.auth.security.CurrentUser;
import com.viandas.api.company.domain.Company;
import com.viandas.api.company.domain.CompanyMembership;
import com.viandas.api.company.persistence.CompanyMembershipRepository;
import com.viandas.api.shared.ApiException;
import com.viandas.api.user.domain.User;
import com.viandas.api.user.domain.UserRole;
import com.viandas.api.user.dto.response.UserContextCompanyResponse;
import com.viandas.api.user.dto.response.UserContextResponse;
import com.viandas.api.user.persistence.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserContextService {
	private final UserRepository userRepository;
	private final CompanyMembershipRepository companyMembershipRepository;

	public UserContextService(UserRepository userRepository, CompanyMembershipRepository companyMembershipRepository) {
		this.userRepository = userRepository;
		this.companyMembershipRepository = companyMembershipRepository;
	}

	@Transactional(readOnly = true)
	public UserContextResponse context(CurrentUser currentUser) {
		User user = userRepository.findById(currentUser.userId())
				.filter(User::isEnabled)
				.orElseThrow(() -> ApiException.unauthorized("User not found"));

		UserContextCompanyResponse company = user.getRole() == UserRole.EMPLOYEE
				? employeeCompany(user)
				: null;
		return new UserContextResponse(
				new AuthUserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole()),
				company);
	}

	private UserContextCompanyResponse employeeCompany(User user) {
		long memberships = companyMembershipRepository.countByUserId(user.getId());
		if (memberships == 0) {
			throw ApiException.forbidden("Employee has no company");
		}
		if (memberships > 1) {
			throw ApiException.conflict("Employee belongs to more than one company");
		}
		Company company = companyMembershipRepository.findByUserId(user.getId())
				.map(CompanyMembership::getCompany)
				.orElseThrow(() -> ApiException.forbidden("Employee has no company"));
		return new UserContextCompanyResponse(company.getId(), company.getName(), company.getSlug());
	}
}
