package com.viandas.api.menu.web;

import java.util.UUID;

import com.viandas.api.menu.domain.*;
import com.viandas.api.menu.application.*;
import com.viandas.api.menu.dto.request.AddMenuItemRequest;
import com.viandas.api.menu.dto.request.CloneMenuRequest;
import com.viandas.api.menu.dto.request.CreateMenuRequest;
import com.viandas.api.menu.dto.response.MenuItemResponse;
import com.viandas.api.menu.dto.response.MenuResponse;
import com.viandas.api.menu.dto.response.PublicMenuResponse;
import com.viandas.api.menu.dto.response.ShareMessageResponse;
import com.viandas.api.shared.ApiResponse;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import com.viandas.api.auth.security.SecurityUtils;

@RestController
@RequestMapping
public class MenuController {
	private final MenuService menuService;

	public MenuController(MenuService menuService) {
		this.menuService = menuService;
	}

	@GetMapping("/menus")
	ApiResponse<List<MenuResponse>> list(
			@RequestParam(required = false) UUID companyId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		return ApiResponse.ok("Menus obtenidos", menuService.list(SecurityUtils.currentUser(), companyId, date));
	}

	@PostMapping("/menus")
	ApiResponse<MenuResponse> create(@Valid @RequestBody CreateMenuRequest request) {
		return ApiResponse.ok("Menu creado", menuService.create(SecurityUtils.currentUser(), request));
	}

	@DeleteMapping("/menus/{id}")
	ApiResponse<Void> delete(@PathVariable UUID id) {
		menuService.delete(SecurityUtils.currentUser(), id);
		return ApiResponse.ok("Menu eliminado", null);
	}

	@PostMapping("/menus/{id}/clone")
	ApiResponse<MenuResponse> clone(@PathVariable UUID id, @Valid @RequestBody CloneMenuRequest request) {
		return ApiResponse.ok("Menu clonado", menuService.clone(SecurityUtils.currentUser(), id, request));
	}

	@GetMapping("/menus/{id}/items")
	ApiResponse<List<MenuItemResponse>> getItems(@PathVariable UUID id) {
		return ApiResponse.ok("Items obtenidos", menuService.getMenuItems(SecurityUtils.currentUser(), id));
	}

	@PostMapping("/menus/{id}/items")
	ApiResponse<MenuItemResponse> addItem(@PathVariable UUID id, @Valid @RequestBody AddMenuItemRequest request) {
		return ApiResponse.ok("Item agregado", menuService.addItem(SecurityUtils.currentUser(), id, request));
	}

	@PatchMapping("/menus/{id}/publish")
	ApiResponse<ShareMessageResponse> publish(@PathVariable UUID id) {
		return ApiResponse.ok("Menu publicado", menuService.publish(SecurityUtils.currentUser(), id));
	}

	@GetMapping("/menus/{id}/share-message")
	ApiResponse<ShareMessageResponse> shareMessage(@PathVariable UUID id) {
		return ApiResponse.ok("Mensaje para compartir obtenido", menuService.shareMessage(SecurityUtils.currentUser(), id));
	}

	@GetMapping("/public/menus/{companySlug}/{date}")
	ApiResponse<PublicMenuResponse> publicMenu(
			@PathVariable String companySlug,
			@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@RequestParam("t") String token) {
		return ApiResponse.ok("Menu publico obtenido", menuService.getPublicMenu(companySlug, date, token));
	}

	@GetMapping("/employee/menus/global/{date}")
	ApiResponse<PublicMenuResponse> employeeGlobalMenu(
			@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@RequestParam("t") String token) {
		return ApiResponse.ok("Menu global obtenido", menuService.getEmployeeGlobalMenu(SecurityUtils.currentUser(), date, token));
	}

}
