package com.viandas.api.menu;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.viandas.api.auth.CurrentUser;
import com.viandas.api.company.Company;
import com.viandas.api.company.CompanyService;
import com.viandas.api.notification.NotificationService;
import com.viandas.api.shared.ApiException;
import com.viandas.api.shared.TokenHasher;

@Service
public class MenuService {
	private final MenuRepository menuRepository;
	private final MenuItemRepository menuItemRepository;
	private final MenuPublicLinkRepository menuPublicLinkRepository;
	private final CompanyService companyService;
	private final TokenHasher tokenHasher;
	private final NotificationService notificationService;
	private final String publicBaseUrl;

	public MenuService(
			MenuRepository menuRepository,
			MenuItemRepository menuItemRepository,
			MenuPublicLinkRepository menuPublicLinkRepository,
			CompanyService companyService,
			TokenHasher tokenHasher,
			NotificationService notificationService,
			@Value("${viandas.public-base-url}") String publicBaseUrl) {
		this.menuRepository = menuRepository;
		this.menuItemRepository = menuItemRepository;
		this.menuPublicLinkRepository = menuPublicLinkRepository;
		this.companyService = companyService;
		this.tokenHasher = tokenHasher;
		this.notificationService = notificationService;
		this.publicBaseUrl = publicBaseUrl;
	}

	@Transactional
	public MenuResponse create(CurrentUser currentUser, CreateMenuRequest request) {
		Company company = companyService.requireOwnedCompany(currentUser, request.companyId());
		if (menuRepository.findByCompanyIdAndMenuDate(company.getId(), request.date()).isPresent()) {
			throw ApiException.conflict("Menu already exists for this company/date");
		}
		Menu menu = menuRepository.save(new Menu(company, request.date(), request.orderClosesAt()));
		return toMenuResponse(menu, List.of());
	}

	@Transactional
	public MenuItemResponse addItem(CurrentUser currentUser, Long menuId, AddMenuItemRequest request) {
		Menu menu = requireOwnedMenu(currentUser, menuId);
		MenuItem item = new MenuItem(menu, request.name().trim(), request.price(), request.category());
		item.setPhotoUrl(request.photoUrl());
		item.setRemainingStock(request.remainingStock());
		return toItemResponse(menuItemRepository.save(item));
	}

	@Transactional
	public ShareMessageResponse publish(CurrentUser currentUser, Long menuId) {
		Menu menu = requireOwnedMenu(currentUser, menuId);
		menu.setStatus(MenuStatus.PUBLISHED);
		menu.setPublishedAt(Instant.now());
		menu.setUpdatedAt(Instant.now());
		menuRepository.save(menu);
		ShareMessageResponse response = createShareMessage(menu);
		notificationService.notifyUser(menu.getCompany().getCook().getId(), "Menu published", "Menu published for " + menu.getCompany().getName(), Map.of());
		return response;
	}

	@Transactional
	public ShareMessageResponse shareMessage(CurrentUser currentUser, Long menuId) {
		Menu menu = requireOwnedMenu(currentUser, menuId);
		if (menu.getStatus() != MenuStatus.PUBLISHED) {
			throw ApiException.conflict("Menu must be published before sharing");
		}
		return createShareMessage(menu);
	}

	public PublicMenuResponse getPublicMenu(String companySlug, LocalDate date, String token) {
		PublicMenuAccess access = validatePublicAccess(companySlug, date, token);
		List<MenuItem> items = menuItemRepository.findByMenuIdOrderByCategoryAscIdAsc(access.menu().getId());
		return toPublicMenuResponse(access.menu(), items);
	}

	public PublicMenuAccess validatePublicAccess(String companySlug, LocalDate date, String token) {
		if (token == null || token.isBlank()) {
			throw ApiException.unauthorized("Public token is required");
		}
		MenuPublicLink link = menuPublicLinkRepository.findByTokenHashAndActiveTrue(tokenHasher.hash(token))
				.orElseThrow(() -> ApiException.unauthorized("Invalid public token"));
		if (link.getExpiresAt().isBefore(Instant.now())) {
			throw ApiException.unauthorized("Public token expired");
		}
		Menu menu = link.getMenu();
		if (!menu.getCompany().getSlug().equals(companySlug) || !menu.getMenuDate().equals(date)) {
			throw ApiException.unauthorized("Public token does not match menu");
		}
		if (menu.getStatus() != MenuStatus.PUBLISHED) {
			throw ApiException.conflict("Menu is not published");
		}
		return new PublicMenuAccess(menu, link);
	}

	public Menu requireOwnedMenu(CurrentUser currentUser, Long menuId) {
		if (!currentUser.isCook()) {
			throw ApiException.forbidden("Cook role required");
		}
		return menuRepository.findByIdAndCompanyCookId(menuId, currentUser.userId())
				.orElseThrow(() -> ApiException.notFound("Menu not found"));
	}

	private ShareMessageResponse createShareMessage(Menu menu) {
		menuPublicLinkRepository.findByMenuIdAndActiveTrue(menu.getId()).forEach(link -> link.setActive(false));
		String token = tokenHasher.newToken();
		MenuPublicLink link = menuPublicLinkRepository.save(new MenuPublicLink(
				menu,
				menu.getCompany(),
				tokenHasher.hash(token),
				Instant.now().plusSeconds(36 * 60 * 60)));
		String url = publicUrl(menu, token);
		List<MenuItem> items = menuItemRepository.findByMenuIdOrderByCategoryAscIdAsc(menu.getId());
		String text = buildShareText(menu, items, url);
		return new ShareMessageResponse(link.getId(), url, text);
	}

	private String publicUrl(Menu menu, String token) {
		return publicBaseUrl + "/m/" + menu.getCompany().getSlug() + "/" + menu.getMenuDate() + "?t=" + token;
	}

	private String buildShareText(Menu menu, List<MenuItem> items, String url) {
		StringBuilder builder = new StringBuilder();
		builder.append("Buenos dias!!!\nLes mando las comidas de hoy\n\n");
		Map<MenuItemCategory, List<MenuItem>> grouped = items.stream()
				.sorted(Comparator.comparing(MenuItem::getCategory).thenComparing(MenuItem::getId))
				.collect(Collectors.groupingBy(MenuItem::getCategory, Collectors.toList()));
		appendCategory(builder, "Plato del dia", grouped.get(MenuItemCategory.PLATO));
		appendCategory(builder, "Minutas / platos calientes", grouped.get(MenuItemCategory.MINUTA));
		appendCategory(builder, "Ensaladas", grouped.get(MenuItemCategory.ENSALADA));
		builder.append("Pedidos aca:\n").append(url).append("\n\n");
		builder.append("Cierre de pedidos: ").append(menu.getOrderClosesAt()).append(" hs");
		return builder.toString();
	}

	private void appendCategory(StringBuilder builder, String title, List<MenuItem> items) {
		if (items == null || items.isEmpty()) {
			return;
		}
		builder.append(title).append("\n");
		for (MenuItem item : items) {
			builder.append("- ").append(item.getName()).append(" - ").append(formatMoney(item.getPrice())).append("\n");
		}
		builder.append("\n");
	}

	private static String formatMoney(BigDecimal price) {
		NumberFormat format = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-AR"));
		format.setMaximumFractionDigits(0);
		return format.format(price);
	}

	private MenuResponse toMenuResponse(Menu menu, List<MenuItem> items) {
		return new MenuResponse(
				menu.getId(),
				menu.getCompany().getId(),
				menu.getCompany().getName(),
				menu.getMenuDate(),
				menu.getStatus(),
				menu.getOrderClosesAt(),
				items.stream().map(this::toItemResponse).toList());
	}

	private PublicMenuResponse toPublicMenuResponse(Menu menu, List<MenuItem> items) {
		boolean canOrder = menu.getOrderClosesAt().isAfter(LocalTime.now(ZoneId.of("America/Buenos_Aires")));
		return new PublicMenuResponse(
				menu.getId(),
				menu.getCompany().getName(),
				menu.getCompany().getSlug(),
				menu.getMenuDate(),
				menu.getOrderClosesAt(),
				canOrder,
				items.stream().map(this::toItemResponse).toList());
	}

	private MenuItemResponse toItemResponse(MenuItem item) {
		return new MenuItemResponse(
				item.getId(),
				item.getName(),
				item.getPrice(),
				item.getCategory(),
				item.getPhotoUrl(),
				item.getRemainingStock());
	}

	public record CreateMenuRequest(Long companyId, LocalDate date, LocalTime orderClosesAt) {
	}

	public record AddMenuItemRequest(String name, BigDecimal price, MenuItemCategory category, String photoUrl, Integer remainingStock) {
	}

	public record MenuItemResponse(Long id, String name, BigDecimal price, MenuItemCategory category, String photoUrl, Integer remainingStock) {
	}

	public record MenuResponse(
			Long id,
			Long companyId,
			String companyName,
			LocalDate date,
			MenuStatus status,
			LocalTime orderClosesAt,
			List<MenuItemResponse> items) {
	}

	public record PublicMenuResponse(
			Long id,
			String companyName,
			String companySlug,
			LocalDate date,
			LocalTime orderClosesAt,
			boolean canOrder,
			List<MenuItemResponse> items) {
	}

	public record ShareMessageResponse(Long publicLinkId, String publicUrl, String whatsappText) {
	}

	public record PublicMenuAccess(Menu menu, MenuPublicLink link) {
	}
}
