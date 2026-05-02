package com.viandas.api.menu.web;

import com.viandas.api.menu.domain.*;
import com.viandas.api.menu.application.*;
import com.viandas.api.menu.dto.request.AddMenuItemRequest;
import com.viandas.api.menu.dto.request.CreateMenuRequest;
import com.viandas.api.menu.dto.response.MenuItemResponse;
import com.viandas.api.menu.dto.response.MenuResponse;
import com.viandas.api.menu.dto.response.PublicMenuResponse;
import com.viandas.api.menu.dto.response.ShareMessageResponse;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.viandas.api.auth.security.SecurityUtils;

@RestController
@RequestMapping
public class MenuController {
	private final MenuService menuService;

	public MenuController(MenuService menuService) {
		this.menuService = menuService;
	}

	@GetMapping("/menus")
	List<MenuResponse> list(
			@RequestParam(required = false) Long companyId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		return menuService.list(SecurityUtils.currentUser(), companyId, date);
	}

	@PostMapping("/menus")
	MenuResponse create(@Valid @RequestBody CreateMenuRequest request) {
		return menuService.create(SecurityUtils.currentUser(), request);
	}

	@PostMapping("/menus/{id}/items")
	MenuItemResponse addItem(@PathVariable Long id, @Valid @RequestBody AddMenuItemRequest request) {
		return menuService.addItem(SecurityUtils.currentUser(), id, request);
	}

	@PatchMapping("/menus/{id}/publish")
	ShareMessageResponse publish(@PathVariable Long id) {
		return menuService.publish(SecurityUtils.currentUser(), id);
	}

	@GetMapping("/menus/{id}/share-message")
	ShareMessageResponse shareMessage(@PathVariable Long id) {
		return menuService.shareMessage(SecurityUtils.currentUser(), id);
	}

	@GetMapping("/public/menus/{companySlug}/{date}")
	PublicMenuResponse publicMenu(
			@PathVariable String companySlug,
			@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@RequestParam("t") String token) {
		return menuService.getPublicMenu(companySlug, date, token);
	}
}
