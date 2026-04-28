package com.viandas.api.menu;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.viandas.api.auth.SecurityUtils;

@RestController
@RequestMapping
public class MenuController {
	private final MenuService menuService;

	public MenuController(MenuService menuService) {
		this.menuService = menuService;
	}

	@PostMapping("/menus")
	MenuService.MenuResponse create(@RequestBody MenuService.CreateMenuRequest request) {
		return menuService.create(SecurityUtils.currentUser(), request);
	}

	@PostMapping("/menus/{id}/items")
	MenuService.MenuItemResponse addItem(@PathVariable Long id, @RequestBody MenuService.AddMenuItemRequest request) {
		return menuService.addItem(SecurityUtils.currentUser(), id, request);
	}

	@PatchMapping("/menus/{id}/publish")
	MenuService.ShareMessageResponse publish(@PathVariable Long id) {
		return menuService.publish(SecurityUtils.currentUser(), id);
	}

	@GetMapping("/menus/{id}/share-message")
	MenuService.ShareMessageResponse shareMessage(@PathVariable Long id) {
		return menuService.shareMessage(SecurityUtils.currentUser(), id);
	}

	@GetMapping("/public/menus/{companySlug}/{date}")
	MenuService.PublicMenuResponse publicMenu(
			@PathVariable String companySlug,
			@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@RequestParam("t") String token) {
		return menuService.getPublicMenu(companySlug, date, token);
	}
}
