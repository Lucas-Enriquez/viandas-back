package com.viandas.api.product.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.viandas.api.auth.security.SecurityUtils;
import com.viandas.api.menu.domain.MenuItemCategory;
import com.viandas.api.product.application.ProductService;
import com.viandas.api.product.dto.request.ProductRequest;
import com.viandas.api.product.dto.response.ProductResponse;
import com.viandas.api.product.dto.response.UploadSignatureResponse;
import com.viandas.api.shared.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/products")
public class ProductController {
	private final ProductService productService;

	public ProductController(ProductService productService) {
		this.productService = productService;
	}

	@GetMapping
	ApiResponse<List<ProductResponse>> list(@RequestParam(required = false) MenuItemCategory category) {
		return ApiResponse.ok("Productos obtenidos", productService.list(SecurityUtils.currentUser(), category));
	}

	@GetMapping("/{id}")
	ApiResponse<ProductResponse> get(@PathVariable UUID id) {
		return ApiResponse.ok("Producto obtenido", productService.get(SecurityUtils.currentUser(), id));
	}

	@PostMapping
	ApiResponse<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
		return ApiResponse.ok("Producto creado", productService.create(SecurityUtils.currentUser(), request));
	}

	@PostMapping("/uploads/sign")
	ApiResponse<UploadSignatureResponse> signUpload() {
		return ApiResponse.ok("Firma de subida generada", productService.signUpload(SecurityUtils.currentUser()));
	}

	@PatchMapping("/{id}")
	ApiResponse<ProductResponse> update(@PathVariable UUID id, @Valid @RequestBody ProductRequest request) {
		return ApiResponse.ok("Producto actualizado", productService.update(SecurityUtils.currentUser(), id, request));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@PathVariable UUID id) {
		productService.delete(SecurityUtils.currentUser(), id);
	}
}
