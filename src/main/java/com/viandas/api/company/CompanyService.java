package com.viandas.api.company;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.viandas.api.auth.CurrentUser;
import com.viandas.api.shared.ApiException;
import com.viandas.api.shared.SlugGenerator;
import com.viandas.api.user.User;
import com.viandas.api.user.UserRepository;

@Service
public class CompanyService {
	private final CompanyRepository companyRepository;
	private final CompanyLocationRepository companyLocationRepository;
	private final UserRepository userRepository;
	private final SlugGenerator slugGenerator;

	public CompanyService(
			CompanyRepository companyRepository,
			CompanyLocationRepository companyLocationRepository,
			UserRepository userRepository,
			SlugGenerator slugGenerator) {
		this.companyRepository = companyRepository;
		this.companyLocationRepository = companyLocationRepository;
		this.userRepository = userRepository;
		this.slugGenerator = slugGenerator;
	}

	public List<CompanyResponse> list(CurrentUser currentUser) {
		requireCook(currentUser);
		return companyRepository.findByCookIdOrderByName(currentUser.userId()).stream().map(this::toResponse).toList();
	}

	public CompanyResponse get(CurrentUser currentUser, Long id) {
		return toResponse(requireOwnedCompany(currentUser, id));
	}

	@Transactional
	public CompanyResponse create(CurrentUser currentUser, CompanyRequest request) {
		requireCook(currentUser);
		User cook = userRepository.findById(currentUser.userId()).orElseThrow(() -> ApiException.unauthorized("User not found"));
		Company company = new Company(cook, request.name().trim(), uniqueSlug(request.name()));
		apply(company, request);
		Company saved = companyRepository.save(company);
		companyLocationRepository.save(new CompanyLocation(saved));
		return toResponse(saved);
	}

	@Transactional
	public CompanyResponse update(CurrentUser currentUser, Long id, CompanyRequest request) {
		Company company = requireOwnedCompany(currentUser, id);
		company.setName(request.name().trim());
		apply(company, request);
		company.setUpdatedAt(Instant.now());
		companyLocationRepository.save(new CompanyLocation(company));
		return toResponse(company);
	}

	@Transactional
	public CompanyResponse updateLocation(CurrentUser currentUser, Long id, CompanyLocationRequest request) {
		Company company = requireOwnedCompany(currentUser, id);
		company.setAddress(request.address());
		company.setLatitude(request.latitude());
		company.setLongitude(request.longitude());
		company.setLocationSource(request.locationSource() == null ? LocationSource.MANUAL : request.locationSource());
		company.setUpdatedAt(Instant.now());
		companyLocationRepository.save(new CompanyLocation(company));
		return toResponse(company);
	}

	public Company requireOwnedCompany(CurrentUser currentUser, Long id) {
		requireCook(currentUser);
		return companyRepository.findByIdAndCookId(id, currentUser.userId())
				.orElseThrow(() -> ApiException.notFound("Company not found"));
	}

	private void apply(Company company, CompanyRequest request) {
		company.setAddress(request.address());
		company.setNotes(request.notes());
		company.setLatitude(request.latitude());
		company.setLongitude(request.longitude());
		company.setLocationSource(request.locationSource());
		company.setWhatsappGroupLabel(request.whatsappGroupLabel());
	}

	private String uniqueSlug(String name) {
		String base = slugGenerator.slugify(name);
		String slug = base;
		int suffix = 2;
		while (companyRepository.existsBySlug(slug)) {
			slug = base + "-" + suffix++;
		}
		return slug;
	}

	private static void requireCook(CurrentUser currentUser) {
		if (!currentUser.isCook()) {
			throw ApiException.forbidden("Cook role required");
		}
	}

	private CompanyResponse toResponse(Company company) {
		return new CompanyResponse(
				company.getId(),
				company.getName(),
				company.getSlug(),
				company.getAddress(),
				company.getNotes(),
				company.getLatitude(),
				company.getLongitude(),
				company.getLocationSource(),
				company.getWhatsappGroupLabel());
	}

	public record CompanyRequest(
			String name,
			String address,
			String notes,
			BigDecimal latitude,
			BigDecimal longitude,
			LocationSource locationSource,
			String whatsappGroupLabel) {
	}

	public record CompanyLocationRequest(
			String address,
			BigDecimal latitude,
			BigDecimal longitude,
			LocationSource locationSource) {
	}

	public record CompanyResponse(
			Long id,
			String name,
			String slug,
			String address,
			String notes,
			BigDecimal latitude,
			BigDecimal longitude,
			LocationSource locationSource,
			String whatsappGroupLabel) {
	}
}
