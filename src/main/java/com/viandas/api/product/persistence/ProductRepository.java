package com.viandas.api.product.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.viandas.api.menu.domain.MenuItemCategory;
import com.viandas.api.product.domain.Product;

public interface ProductRepository extends JpaRepository<Product, UUID> {
	Optional<Product> findByIdAndCookId(UUID id, UUID cookId);

	List<Product> findByCookIdOrderByCategoryAscNameAsc(UUID cookId);

	List<Product> findByCookIdAndCategoryOrderByNameAsc(UUID cookId, MenuItemCategory category);

	boolean existsByCookIdAndNameIgnoreCase(UUID cookId, String name);

	boolean existsByCookIdAndNameIgnoreCaseAndIdNot(UUID cookId, String name, UUID id);
}
