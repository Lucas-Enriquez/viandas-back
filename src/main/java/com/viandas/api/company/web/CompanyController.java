package com.viandas.api.company.web;

import java.util.UUID;

import java.util.List;

import com.viandas.api.auth.security.SecurityUtils;
import com.viandas.api.company.application.CompanyService;
import com.viandas.api.company.application.GeocodingService;
import com.viandas.api.company.dto.request.CompanyLocationRequest;
import com.viandas.api.company.dto.request.CompanyRequest;
import com.viandas.api.company.dto.request.GeocodePreviewRequest;
import com.viandas.api.company.dto.response.CompanyResponse;
import com.viandas.api.company.dto.response.GeocodePreviewResponse;
import com.viandas.api.shared.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
    ApiResponse<List<CompanyResponse>> list() {
        return ApiResponse.ok("Companias obtenidas", companyService.list(SecurityUtils.currentUser()));
    }

    @PostMapping
    ApiResponse<CompanyResponse> create(@Valid @RequestBody CompanyRequest request) {
        return ApiResponse.ok("Compania creada", companyService.create(SecurityUtils.currentUser(), request));
    }

    @GetMapping("/{id}")
    ApiResponse<CompanyResponse> get(@PathVariable UUID id) {
        return ApiResponse.ok("Compania obtenida", companyService.get(SecurityUtils.currentUser(), id));
    }

    @PatchMapping("/{id}")
    ApiResponse<CompanyResponse> update(@PathVariable UUID id, @Valid @RequestBody CompanyRequest request) {
        return ApiResponse.ok("Compania actualizada", companyService.update(SecurityUtils.currentUser(), id, request));
    }

    @PatchMapping("/{id}/location")
    ApiResponse<CompanyResponse> updateLocation(@PathVariable UUID id, @Valid @RequestBody CompanyLocationRequest request) {
        return ApiResponse.ok("Ubicacion actualizada", companyService.updateLocation(SecurityUtils.currentUser(), id, request));
    }

    @PostMapping("/{id}/location/geocode-preview")
    ApiResponse<GeocodePreviewResponse> geocodePreview(@PathVariable UUID id, @Valid @RequestBody GeocodePreviewRequest request) {
        companyService.requireOwnedCompany(SecurityUtils.currentUser(), id);
        return ApiResponse.ok("Preview de geocoding obtenido", geocodingService.preview(request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID id) {
        companyService.delete(SecurityUtils.currentUser(), id);
    }
}
