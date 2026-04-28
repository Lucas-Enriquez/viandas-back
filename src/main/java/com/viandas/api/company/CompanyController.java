package com.viandas.api.company;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viandas.api.auth.SecurityUtils;

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
	List<CompanyService.CompanyResponse> list() {
		return companyService.list(SecurityUtils.currentUser());
	}

	@PostMapping
	CompanyService.CompanyResponse create(@RequestBody CompanyService.CompanyRequest request) {
		return companyService.create(SecurityUtils.currentUser(), request);
	}

	@GetMapping("/{id}")
	CompanyService.CompanyResponse get(@PathVariable Long id) {
		return companyService.get(SecurityUtils.currentUser(), id);
	}

	@PatchMapping("/{id}")
	CompanyService.CompanyResponse update(@PathVariable Long id, @RequestBody CompanyService.CompanyRequest request) {
		return companyService.update(SecurityUtils.currentUser(), id, request);
	}

	@PatchMapping("/{id}/location")
	CompanyService.CompanyResponse updateLocation(@PathVariable Long id, @RequestBody CompanyService.CompanyLocationRequest request) {
		return companyService.updateLocation(SecurityUtils.currentUser(), id, request);
	}

	@PostMapping("/{id}/location/geocode-preview")
	GeocodingService.GeocodePreviewResponse geocodePreview(@PathVariable Long id, @RequestBody GeocodingService.GeocodePreviewRequest request) {
		companyService.requireOwnedCompany(SecurityUtils.currentUser(), id);
		return geocodingService.preview(request);
	}
}
