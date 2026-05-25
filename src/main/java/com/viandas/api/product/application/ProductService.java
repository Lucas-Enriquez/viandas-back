package com.viandas.api.product.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.viandas.api.auth.security.CurrentUser;
import com.viandas.api.menu.domain.MenuItem;
import com.viandas.api.menu.domain.MenuItemCategory;
import com.viandas.api.menu.domain.MenuStatus;
import com.viandas.api.menu.persistence.MenuItemRepository;
import com.viandas.api.product.domain.Product;
import com.viandas.api.product.dto.request.ProductCreateRequest;
import com.viandas.api.product.dto.request.ProductPatchRequest;
import com.viandas.api.product.dto.response.ProductResponse;
import com.viandas.api.product.dto.response.UploadSignatureResponse;
import com.viandas.api.product.persistence.ProductRepository;
import com.viandas.api.shared.ApiException;
import com.viandas.api.shared.photo.PhotoUploadService;
import com.viandas.api.user.domain.User;
import com.viandas.api.user.persistence.UserRepository;

@Service
public class ProductService {
	private final ProductRepository productRepository;
	private final UserRepository userRepository;
	private final PhotoUploadService photoUploadService;
	private final MenuItemRepository menuItemRepository;

	public ProductService(
			ProductRepository productRepository,
			UserRepository userRepository,
			PhotoUploadService photoUploadService,
			MenuItemRepository menuItemRepository) {
		this.productRepository = productRepository;
		this.userRepository = userRepository;
		this.photoUploadService = photoUploadService;
		this.menuItemRepository = menuItemRepository;
	}

	public List<ProductResponse> list(CurrentUser currentUser, MenuItemCategory category) {
		requireCook(currentUser);
		List<Product> products = category == null
				? productRepository.findByCookIdOrderByCategoryAscNameAsc(currentUser.userId())
				: productRepository.findByCookIdAndCategoryOrderByNameAsc(currentUser.userId(), category);
		return products.stream().map(this::toResponse).toList();
	}

	public ProductResponse get(CurrentUser currentUser, UUID id) {
		return toResponse(requireOwnedProduct(currentUser, id));
	}

	@Transactional
	public ProductResponse create(CurrentUser currentUser, ProductCreateRequest request) {
		requireCook(currentUser);
		String name = request.name().trim();
		if (productRepository.existsByCookIdAndNameIgnoreCase(currentUser.userId(), name)) {
			throw ApiException.conflict("Ya existe un producto con ese nombre");
		}
		User cook = userRepository.findById(currentUser.userId())
				.orElseThrow(() -> ApiException.unauthorized("User not found"));
		Product product = new Product(cook, name, request.price(), request.category());
		applyPhoto(product, request.photoPublicId());
		product.setDescription(request.description());
		return toResponse(productRepository.save(product));
	}

	@Transactional
	public ProductResponse update(CurrentUser currentUser, UUID id, ProductPatchRequest request) {
		Product product = requireOwnedProduct(currentUser, id);

		if (request.name() != null) {
			String name = request.name().trim();
			if (name.isEmpty()) {
				throw ApiException.badRequest("El nombre no puede estar vacio");
			}
			if (productRepository.existsByCookIdAndNameIgnoreCaseAndIdNot(currentUser.userId(), name, id)) {
				throw ApiException.conflict("Ya existe un producto con ese nombre");
			}
			product.setName(name);
		}

		if (request.price() != null) {
			product.setPrice(request.price());
		}

		if (request.category() != null) {
			product.setCategory(request.category());
		}

		if (request.photoPublicId() != null) {
			applyPhoto(product, request.photoPublicId());
		}

		if (request.description() != null) {
			product.setDescription(request.description());
		}

		product.setUpdatedAt(Instant.now());
		syncDraftMenuItems(product);
		return toResponse(product);
	}

	/**
	 * Propaga los datos actuales del producto a los MenuItems que lo referencian
	 * y cuyo menu esta en DRAFT. Los items de menus publicados quedan intactos
	 * (snapshot historico).
	 */
	private void syncDraftMenuItems(Product product) {
		List<MenuItem> items = menuItemRepository.findByProductIdAndMenuStatus(product.getId(), MenuStatus.DRAFT);
		for (MenuItem item : items) {
			item.setName(product.getName());
			item.setPrice(product.getPrice());
			item.setCategory(product.getCategory());
			item.setPhotoUrl(product.getPhotoUrl());
		}
	}

	@Transactional
	public void delete(CurrentUser currentUser, UUID id) {
		requireOwnedProduct(currentUser, id);
		// Snapshot semantics: el asset en Cloudinary queda vivo aunque se borre el Product,
		// porque MenuItems anteriores guardaron el URL como snapshot.
		productRepository.deleteById(id);
	}

	public UploadSignatureResponse signUpload(CurrentUser currentUser) {
		requireCook(currentUser);
		PhotoUploadService.UploadSignature signature = photoUploadService.signProductUpload();
		return new UploadSignatureResponse(
				signature.cloudName(),
				signature.apiKey(),
				signature.timestamp(),
				signature.folder(),
				signature.signature(),
				signature.uploadUrl());
	}

	public Product requireOwnedProduct(CurrentUser currentUser, UUID id) {
		requireCook(currentUser);
		return productRepository.findByIdAndCookId(id, currentUser.userId())
				.orElseThrow(() -> ApiException.notFound("Producto no encontrado"));
	}

	/**
	 * Applies the photoPublicId to the entity:
	 * <ul>
	 *     <li>null/blank → clear both fields (lo viejo queda en Cloudinary, snapshot semántica).</li>
	 *     <li>non-blank → validar pertenencia a la carpeta + setear publicId y URL derivado.</li>
	 * </ul>
	 */
	private void applyPhoto(Product product, String photoPublicId) {
		if (photoPublicId == null || photoPublicId.isBlank()) {
			product.setPhotoPublicId(null);
			product.setPhotoUrl(null);
			return;
		}
		photoUploadService.validateProductPublicId(photoPublicId);
		product.setPhotoPublicId(photoPublicId);
		product.setPhotoUrl(photoUploadService.buildDeliveryUrl(photoPublicId));
	}

	private static void requireCook(CurrentUser currentUser) {
		if (!currentUser.isCook()) {
			throw ApiException.forbidden("Cook role required");
		}
	}

	private ProductResponse toResponse(Product product) {
		return new ProductResponse(
				product.getId(),
				product.getName(),
				product.getPrice(),
				product.getCategory(),
				product.getPhotoUrl(),
				product.getDescription(),
				product.getCreatedAt(),
				product.getUpdatedAt());
	}
}
