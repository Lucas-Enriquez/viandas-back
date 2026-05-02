package com.viandas.api.company.web;

import com.viandas.api.company.application.*;
import com.viandas.api.company.dto.request.CompanyLocationRequest;
import com.viandas.api.company.dto.request.CompanyRequest;
import com.viandas.api.company.dto.request.GeocodePreviewRequest;
import com.viandas.api.company.dto.response.CompanyResponse;
import com.viandas.api.company.dto.response.GeocodePreviewResponse;
import java.util.List;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viandas.api.auth.security.SecurityUtils;

@RestController
@RequestMapping("/companies")
public class CompanyController {
	private final CompanyService companyService;
	private final GeocodingService geocodingService;

	public CompanyController(CompanyService companyService, GeocodingService geocodingService) {
		this.companyService = companyService;
		this.geocodingService = geocodingService;
	}

	@GetMapping
	List<CompanyResponse> list() {
		return companyService.list(SecurityUtils.currentUser());
	}

	@PostMapping
	CompanyResponse create(@Valid @RequestBody CompanyRequest request) {
		return companyService.create(SecurityUtils.currentUser(), request);
	}

	@GetMapping("/{id}")
	CompanyResponse get(@PathVariable Long id) {
		return companyService.get(SecurityUtils.currentUser(), id);
	}

	@PatchMapping("/{id}")
	CompanyResponse update(@PathVariable Long id, @Valid @RequestBody CompanyRequest request) {
		return companyService.update(SecurityUtils.currentUser(), id, request);
	}

	@PatchMapping("/{id}/location")
	CompanyResponse updateLocation(@PathVariable Long id, @Valid @RequestBody CompanyLocationRequest request) {
		return companyService.updateLocation(SecurityUtils.currentUser(), id, request);
	}

	@PostMapping("/{id}/location/geocode-preview")
	GeocodePreviewResponse geocodePreview(@PathVariable Long id, @Valid @RequestBody GeocodePreviewRequest request) {
		companyService.requireOwnedCompany(SecurityUtils.currentUser(), id);
		return geocodingService.preview(request);
	}
}
