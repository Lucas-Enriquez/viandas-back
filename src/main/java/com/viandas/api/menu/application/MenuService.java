package com.viandas.api.menu.application;

import java.util.UUID;

import com.viandas.api.auth.security.CurrentUser;
import com.viandas.api.company.application.CompanyService;
import com.viandas.api.company.domain.Company;
import com.viandas.api.company.domain.CompanyMembership;
import com.viandas.api.company.persistence.CompanyMembershipRepository;
import com.viandas.api.company.persistence.CompanyRepository;
import com.viandas.api.menu.domain.Menu;
import com.viandas.api.menu.domain.MenuItem;
import com.viandas.api.menu.domain.MenuItemCategory;
import com.viandas.api.menu.domain.MenuScope;
import com.viandas.api.menu.domain.MenuStatus;
import com.viandas.api.menu.dto.request.AddMenuItemRequest;
import com.viandas.api.menu.dto.request.CloneMenuRequest;
import com.viandas.api.menu.dto.request.CreateMenuRequest;
import com.viandas.api.menu.dto.response.MenuCompanyResponse;
import com.viandas.api.menu.dto.response.MenuItemResponse;
import com.viandas.api.menu.dto.response.MenuResponse;
import com.viandas.api.menu.dto.response.PublicMenuResponse;
import com.viandas.api.menu.dto.response.ShareMessageResponse;
import com.viandas.api.menu.persistence.MenuItemRepository;
import com.viandas.api.menu.persistence.MenuRepository;
import com.viandas.api.notification.application.NotificationService;
import com.viandas.api.order.persistence.OrderRepository;
import com.viandas.api.product.application.ProductService;
import com.viandas.api.product.domain.Product;
import com.viandas.api.shared.ApiException;
import com.viandas.api.shared.photo.PhotoUploadService;
import com.viandas.api.user.domain.User;
import com.viandas.api.user.domain.UserRole;
import com.viandas.api.user.persistence.UserRepository;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MenuService {
    private final MenuRepository menuRepository;
    private final MenuItemRepository menuItemRepository;
    private final CompanyService companyService;
    private final CompanyRepository companyRepository;
    private final CompanyMembershipRepository companyMembershipRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final String publicBaseUrl;
    private final OrderRepository orderRepository;
    private final ProductService productService;
    private final PhotoUploadService photoUploadService;

    public MenuService(
            MenuRepository menuRepository,
            MenuItemRepository menuItemRepository,
            CompanyService companyService,
            CompanyRepository companyRepository,
            CompanyMembershipRepository companyMembershipRepository,
            UserRepository userRepository,
            NotificationService notificationService,
            @Value("${viandas.public-base-url}") String publicBaseUrl,
            OrderRepository orderRepository,
            ProductService productService,
            PhotoUploadService photoUploadService
    ) {
        this.menuRepository = menuRepository;
        this.menuItemRepository = menuItemRepository;
        this.companyService = companyService;
        this.companyRepository = companyRepository;
        this.companyMembershipRepository = companyMembershipRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.publicBaseUrl = publicBaseUrl;
        this.orderRepository = orderRepository;
        this.productService = productService;
        this.photoUploadService = photoUploadService;
    }

    @Transactional
    public MenuResponse create(CurrentUser currentUser, CreateMenuRequest request) {
        MenuScope scope = request.scope() == null ? MenuScope.COMPANY : request.scope();
        return scope == MenuScope.GLOBAL
                ? createGlobal(currentUser, request)
                : createCompany(currentUser, request);
    }

    private MenuResponse createCompany(CurrentUser currentUser, CreateMenuRequest request) {
        if (request.companyId() == null) {
            throw ApiException.badRequest("Company is required");
        }
        Company company = companyService.requireOwnedCompany(currentUser, request.companyId());
        if (menuRepository.findByCompanyIdAndMenuDate(company.getId(), request.date()).isPresent()) {
            throw ApiException.conflict("Menu already exists for this company/date");
        }
        Menu menu = menuRepository.save(new Menu(company, request.date(), request.orderClosesAt()));
        return toMenuResponse(menu, List.of());
    }

    private MenuResponse createGlobal(CurrentUser currentUser, CreateMenuRequest request) {
        if (!currentUser.isCook()) {
            throw ApiException.forbidden("Cook role required");
        }
        if (request.companyIds() == null || request.companyIds().isEmpty()) {
            throw ApiException.badRequest("Companies are required for a global menu");
        }
        if (menuRepository.findByCookIdAndScopeAndMenuDate(currentUser.userId(), MenuScope.GLOBAL, request.date())
                .isPresent()) {
            throw ApiException.conflict("Global menu already exists for this date");
        }

        User cook = userRepository.findById(currentUser.userId())
                .orElseThrow(() -> ApiException.unauthorized("User not found"));
        List<UUID> companyIds = distinctIds(request.companyIds());
        if (companyIds.isEmpty()) {
            throw ApiException.badRequest("Companies are required for a global menu");
        }
        List<Company> companies = companyRepository.findByIdInAndCookId(companyIds, currentUser.userId());
        if (companies.size() != companyIds.size()) {
            throw ApiException.badRequest("One or more companies do not belong to the cook");
        }

        Menu menu = new Menu(cook, request.date(), request.orderClosesAt());
        menu.getAssignedCompanies().addAll(companies);
        Menu saved = menuRepository.save(menu);
        return toMenuResponse(saved, List.of());
    }

    @Transactional
    public void delete(CurrentUser currentUser, UUID id) {

        Menu menu = requireOwnedMenu(currentUser, id);

        if (orderRepository.existsByMenuId(menu.getId())) {
            throw ApiException.conflict("No se puede eliminar un menú con pedidos");
        }

        menuRepository.delete(menu);
    }

    @Transactional
    public MenuResponse clone(CurrentUser currentUser, UUID sourceId, CloneMenuRequest request) {
        Menu source = requireOwnedMenu(currentUser, sourceId);
        LocalTime closesAt = request.orderClosesAt() != null ? request.orderClosesAt() : source.getOrderClosesAt();

        Menu cloned;
        if (source.getScope() == MenuScope.GLOBAL) {
            if (menuRepository.findByCookIdAndScopeAndMenuDate(currentUser.userId(), MenuScope.GLOBAL, request.date()).isPresent()) {
                throw ApiException.conflict("Ya existe un menú global para esa fecha");
            }
            cloned = new Menu(source.getCook(), request.date(), closesAt);
            cloned.getAssignedCompanies().addAll(source.getAssignedCompanies());
        } else {
            if (menuRepository.findByCompanyIdAndMenuDate(source.getCompany().getId(), request.date()).isPresent()) {
                throw ApiException.conflict("Ya existe un menú para esa empresa/fecha");
            }
            cloned = new Menu(source.getCompany(), request.date(), closesAt);
        }

        Menu saved = menuRepository.save(cloned);

        List<MenuItem> clonedItems = source.getItems().stream().map(original -> {
            MenuItem item = new MenuItem(saved, original.getName(), original.getPrice(), original.getCategory());
            item.setPhotoUrl(original.getPhotoUrl());
            item.setRemainingStock(original.getRemainingStock());
            item.setProduct(original.getProduct());
            item.getAvailableCompanies().addAll(original.getAvailableCompanies());
            return item;
        }).toList();

        menuItemRepository.saveAll(clonedItems);
        saved.getItems().addAll(clonedItems);
        return toMenuResponse(saved, clonedItems);
    }

    @Transactional(readOnly = true)
    public List<MenuResponse> list(CurrentUser currentUser, UUID companyId, LocalDate date) {
        if (!currentUser.isCook()) {
            throw ApiException.forbidden("Cook role required");
        }

        List<Menu> menus;
        if (companyId != null && date != null) {
            companyRepository.findByIdAndCookId(companyId, currentUser.userId())
                    .orElseThrow(() -> ApiException.notFound("Company not found"));
            menus = menuRepository.findVisibleByCookAndCompanyAndDate(currentUser.userId(), companyId, date);
        } else if (companyId != null) {
            companyRepository.findByIdAndCookId(companyId, currentUser.userId())
                    .orElseThrow(() -> ApiException.notFound("Company not found"));
            menus = menuRepository.findVisibleByCookAndCompany(currentUser.userId(), companyId);
        } else if (date != null) {
            menus = menuRepository.findByCookIdAndMenuDateOrderByIdDesc(currentUser.userId(), date);
        } else {
            menus = menuRepository.findByCookIdOrderByMenuDateDescIdDesc(currentUser.userId());
        }

        return menus.stream()
                .map(menu -> toMenuResponse(menu, menu.getItems()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MenuItemResponse> getMenuItems(CurrentUser currentUser, UUID menuId) {
        Menu menu = requireOwnedMenu(currentUser, menuId);
        return menu.getItems().stream()
                .map(this::toItemResponse)
                .toList();
    }

    @Transactional
    public void removeItem(CurrentUser currentUser, UUID menuId, UUID itemId) {
        Menu menu = requireOwnedMenu(currentUser, menuId);
        MenuItem item = menuItemRepository.findById(itemId)
                .filter(i -> i.getMenu().getId().equals(menu.getId()))
                .orElseThrow(() -> ApiException.notFound("Item not found"));
        menu.getItems().remove(item);
        menuItemRepository.delete(item);
    }

    @Transactional
    public MenuItemResponse addItem(CurrentUser currentUser, UUID menuId, AddMenuItemRequest request) {
        Menu menu = requireOwnedMenu(currentUser, menuId);
        MenuItem item = request.productId() != null
                ? buildItemFromProduct(currentUser, menu, request)
                : buildFreeFormItem(menu, request);
        item.setRemainingStock(request.remainingStock());

        if (menu.getScope() == MenuScope.GLOBAL) {
            assignItemCompanies(menu, item, request.availableCompanyIds());
        } else if (request.availableCompanyIds() != null && !request.availableCompanyIds().isEmpty()) {
            throw ApiException.badRequest("availableCompanyIds only applies to global menus");
        }

        return toItemResponse(menuItemRepository.save(item));
    }

    private MenuItem buildItemFromProduct(CurrentUser currentUser, Menu menu, AddMenuItemRequest request) {
        if (request.name() != null || request.price() != null
                || request.category() != null || request.photoPublicId() != null) {
            throw ApiException.badRequest("Si envias productId no puedes mandar campos sueltos");
        }
        Product product = productService.requireOwnedProduct(currentUser, request.productId());
        MenuItem item = new MenuItem(menu, product.getName(), product.getPrice(), product.getCategory());
        item.setPhotoUrl(product.getPhotoUrl());
        item.setProduct(product);
        return item;
    }

    private MenuItem buildFreeFormItem(Menu menu, AddMenuItemRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw ApiException.badRequest("El nombre es obligatorio para items sueltos");
        }
        if (request.price() == null) {
            throw ApiException.badRequest("El precio es obligatorio para items sueltos");
        }
        if (request.category() == null) {
            throw ApiException.badRequest("La categoria es obligatoria para items sueltos");
        }
        photoUploadService.validateProductPublicId(request.photoPublicId());
        MenuItem item = new MenuItem(menu, request.name().trim(), request.price(), request.category());
        if (request.photoPublicId() != null && !request.photoPublicId().isBlank()) {
            item.setPhotoUrl(photoUploadService.buildDeliveryUrl(request.photoPublicId()));
        }
        return item;
    }

    @Transactional
    public ShareMessageResponse publish(CurrentUser currentUser, UUID menuId) {
        Menu menu = requireOwnedMenu(currentUser, menuId);
        if (menu.getItems().isEmpty()) {
            throw ApiException.conflict("No se puede publicar un menú sin items");
        }
        boolean firstPublish = menu.getPublishedAt() == null;
        menu.setStatus(MenuStatus.PUBLISHED);
        menu.setPublishedAt(Instant.now());
        menu.setUpdatedAt(Instant.now());
        menuRepository.save(menu);
        String url = publicUrl(menu);
        List<MenuItem> items = menuItemRepository.findByMenuIdOrderByCategoryAscIdAsc(menu.getId());
        String text = buildShareText(menu, items, url);
        notificationService.notifyUser(menu.getCook()
                .getId(), "Menu published", "Menu published for " + menuLabel(menu), Map.of());
        if (firstPublish) {
            notifyEmployeesOfNewMenu(menu);
        }
        return new ShareMessageResponse(url, text);
    }

    private void notifyEmployeesOfNewMenu(Menu menu) {
        List<UUID> companyIds = menu.getScope() == MenuScope.GLOBAL
                ? menu.getAssignedCompanies().stream().map(Company::getId).toList()
                : List.of(menu.getCompany().getId());
        if (companyIds.isEmpty()) {
            return;
        }
        List<CompanyMembership> memberships = companyMembershipRepository.findEmployeesByCompanyIdIn(companyIds);
        if (memberships.isEmpty()) {
            return;
        }
        String title = "Menú disponible";
        String body = "Ya podés pedir el menú del " + menu.getMenuDate();
        Map<String, String> data = Map.of(
                "menuId", menu.getId().toString(),
                "menuDate", menu.getMenuDate().toString());
        for (CompanyMembership membership : memberships) {
            notificationService.notifyUser(membership.getUser().getId(), title, body, data);
        }
    }

    @Transactional
    public ShareMessageResponse shareMessage(CurrentUser currentUser, UUID menuId) {
        Menu menu = requireOwnedMenu(currentUser, menuId);
        if (menu.getStatus() != MenuStatus.PUBLISHED) {
            throw ApiException.conflict("Menu must be published before sharing");
        }
        String url = publicUrl(menu);
        List<MenuItem> items = menuItemRepository.findByMenuIdOrderByCategoryAscIdAsc(menu.getId());
        String text = buildShareText(menu, items, url);
        return new ShareMessageResponse(url, text);
    }

    @Transactional(readOnly = true)
    public PublicMenuResponse getEmployeeMenu(CurrentUser currentUser, LocalDate date) {
        EmployeeMenuAccess access = requireEmployeeMenuAccess(currentUser, date);
        List<MenuItem> items = menuItemRepository.findByMenuIdOrderByCategoryAscIdAsc(access.menu().getId())
                .stream()
                .filter(item -> isItemAvailableForCompany(item, access.company()))
                .toList();
        return toPublicMenuResponse(access.menu(), access.company(), items);
    }

    @Transactional(readOnly = true)
    public EmployeeMenuAccess requireEmployeeMenuAccess(CurrentUser currentUser, LocalDate date) {
        if (currentUser.role() != UserRole.EMPLOYEE) {
            throw ApiException.forbidden("Employee role required");
        }
        Company company = requireEmployeeCompany(currentUser);
        Menu menu = menuRepository.findPublishedByCompanyAndDate(company.getId(), date)
                .orElseThrow(() -> ApiException.notFound("No hay menú publicado aún, intenta más tarde"));
        return new EmployeeMenuAccess(menu, company);
    }

    public record EmployeeMenuAccess(Menu menu, Company company) {}

    public Menu requireOwnedMenu(CurrentUser currentUser, UUID menuId) {
        if (!currentUser.isCook()) {
            throw ApiException.forbidden("Cook role required");
        }
        return menuRepository.findByIdAndCookId(menuId, currentUser.userId())
                .orElseThrow(() -> ApiException.notFound("Menu not found"));
    }

    public Menu requireOwnedMenuForCompany(CurrentUser currentUser, UUID menuId, UUID companyId) {
        Menu menu = requireOwnedMenu(currentUser, menuId);
        if (menu.getScope() == MenuScope.COMPANY) {
            if (!menu.getCompany().getId().equals(companyId)) {
                throw ApiException.badRequest("Menu does not belong to company");
            }
        } else if (!isMenuAssignedToCompany(menu, companyId)) {
            throw ApiException.badRequest("Global menu is not assigned to company");
        }
        return menu;
    }

    public boolean isItemAvailableForCompany(MenuItem item, Company company) {
        if (item.getMenu().getScope() != MenuScope.GLOBAL) {
            return item.getMenu().getCompany().getId().equals(company.getId());
        }
        return item.getAvailableCompanies().isEmpty()
                || item.getAvailableCompanies().stream().anyMatch(assigned -> assigned.getId().equals(company.getId()));
    }

    private void assignItemCompanies(Menu menu, MenuItem item, List<UUID> availableCompanyIds) {
        if (availableCompanyIds == null || availableCompanyIds.isEmpty()) {
            return;
        }
        Set<UUID> assignedCompanyIds = menu.getAssignedCompanies().stream()
                .map(Company::getId)
                .collect(Collectors.toSet());
        List<UUID> requestedIds = distinctIds(availableCompanyIds);
        if (!assignedCompanyIds.containsAll(requestedIds)) {
            throw ApiException.badRequest("One or more item companies are not assigned to the menu");
        }
        Map<UUID, Company> companiesById = menu.getAssignedCompanies().stream()
                .collect(Collectors.toMap(Company::getId, company -> company));
        requestedIds.forEach(id -> item.getAvailableCompanies().add(companiesById.get(id)));
    }

    private Company requireEmployeeCompany(CurrentUser currentUser) {
        long memberships = companyMembershipRepository.countByUserId(currentUser.userId());
        if (memberships == 0) {
            throw ApiException.forbidden("Employee has no company");
        }
        if (memberships > 1) {
            throw ApiException.conflict("Employee belongs to more than one company");
        }
        return companyMembershipRepository.findByUserId(currentUser.userId())
                .map(CompanyMembership::getCompany)
                .orElseThrow(() -> ApiException.forbidden("Employee has no company"));
    }

    private String publicUrl(Menu menu) {
        if (menu.getScope() == MenuScope.GLOBAL) {
            return publicBaseUrl + "/m/global/" + menu.getMenuDate();
        }
        return publicBaseUrl + "/m/" + menu.getCompany().getSlug() + "/" + menu.getMenuDate();
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

    private MenuResponse toMenuResponse(Menu menu, Collection<MenuItem> items) {
        Company company = menu.getScope() == MenuScope.COMPANY ? menu.getCompany() : null;
        return new MenuResponse(
                menu.getId(),
                company == null ? null : company.getId(),
                company == null ? null : company.getName(),
                menu.getScope(),
                menuCompanies(menu),
                menu.getMenuDate(),
                menu.getStatus(),
                menu.getOrderClosesAt(),
                items.stream().map(this::toItemResponse).toList());
    }

    private PublicMenuResponse toPublicMenuResponse(Menu menu, Company company, List<MenuItem> items) {
        boolean canOrder = menu.getOrderClosesAt().isAfter(LocalTime.now(ZoneId.of("America/Buenos_Aires")));
        return new PublicMenuResponse(
                menu.getId(),
                company.getName(),
                company.getSlug(),
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
                item.getRemainingStock(),
                availableCompanyIds(item));
    }

    private List<MenuCompanyResponse> menuCompanies(Menu menu) {
        return menuCompaniesFor(menu).stream()
                .sorted(Comparator.comparing(Company::getName))
                .map(company -> new MenuCompanyResponse(company.getId(), company.getName(), company.getSlug()))
                .toList();
    }

    private Set<Company> menuCompaniesFor(Menu menu) {
        if (menu.getScope() == MenuScope.COMPANY) {
            return Set.of(menu.getCompany());
        }
        return menu.getAssignedCompanies();
    }

    private List<UUID> availableCompanyIds(MenuItem item) {
        Set<Company> companies = item.getAvailableCompanies().isEmpty()
                ? menuCompaniesFor(item.getMenu())
                : item.getAvailableCompanies();
        return companies.stream()
                .map(Company::getId)
                .sorted()
                .toList();
    }

    private boolean isMenuAssignedToCompany(Menu menu, UUID companyId) {
        return menu.getScope() == MenuScope.COMPANY
                ? menu.getCompany().getId().equals(companyId)
                : menu.getAssignedCompanies().stream().anyMatch(company -> company.getId().equals(companyId));
    }

    private static List<UUID> distinctIds(List<UUID> ids) {
        return ids.stream()
                .filter(id -> id != null)
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .toList();
    }

    private String menuLabel(Menu menu) {
        return menu.getScope() == MenuScope.GLOBAL ? "global menu" : menu.getCompany().getName();
    }
}
