package com.viandas.api.company.application;

import java.util.UUID;

import com.viandas.api.company.domain.*;
import com.viandas.api.company.dto.request.CompanyLocationRequest;
import com.viandas.api.company.dto.request.CompanyRequest;
import com.viandas.api.company.dto.response.CompanyResponse;
import com.viandas.api.company.persistence.*;
import com.viandas.api.delivery.persistence.DeliveryLocationUpdateRepository;
import com.viandas.api.delivery.persistence.DeliverySessionRepository;
import com.viandas.api.invitation.persistence.GlobalInvitationRepository;
import com.viandas.api.invitation.persistence.InvitationRepository;
import com.viandas.api.menu.persistence.MenuItemRepository;
import com.viandas.api.menu.persistence.MenuPublicLinkRepository;
import com.viandas.api.menu.persistence.MenuRepository;
import com.viandas.api.notification.persistence.StockBroadcastItemRepository;
import com.viandas.api.notification.persistence.StockBroadcastRepository;
import com.viandas.api.order.persistence.OrderItemRepository;
import com.viandas.api.order.persistence.OrderRepository;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.viandas.api.auth.security.CurrentUser;
import com.viandas.api.shared.ApiException;
import com.viandas.api.shared.SlugGenerator;
import com.viandas.api.user.domain.User;
import com.viandas.api.user.persistence.UserRepository;

@Service
public class CompanyService {
	private final CompanyRepository companyRepository;
	private final CompanyLocationRepository companyLocationRepository;
	private final UserRepository userRepository;
	private final SlugGenerator slugGenerator;
	private final DeliveryLocationUpdateRepository deliveryLocationUpdateRepository;
	private final DeliverySessionRepository deliverySessionRepository;
	private final StockBroadcastItemRepository stockBroadcastItemRepository;
	private final StockBroadcastRepository stockBroadcastRepository;
	private final OrderItemRepository orderItemRepository;
	private final OrderRepository orderRepository;
	private final MenuPublicLinkRepository menuPublicLinkRepository;
	private final MenuItemRepository menuItemRepository;
	private final MenuRepository menuRepository;
	private final InvitationRepository invitationRepository;
	private final GlobalInvitationRepository globalInvitationRepository;
	private final CompanyMembershipRepository companyMembershipRepository;

	public CompanyService(
			CompanyRepository companyRepository,
			CompanyLocationRepository companyLocationRepository,
			UserRepository userRepository,
			SlugGenerator slugGenerator,
			DeliveryLocationUpdateRepository deliveryLocationUpdateRepository,
			DeliverySessionRepository deliverySessionRepository,
			StockBroadcastItemRepository stockBroadcastItemRepository,
			StockBroadcastRepository stockBroadcastRepository,
			OrderItemRepository orderItemRepository,
			OrderRepository orderRepository,
			MenuPublicLinkRepository menuPublicLinkRepository,
			MenuItemRepository menuItemRepository,
			MenuRepository menuRepository,
			InvitationRepository invitationRepository,
			GlobalInvitationRepository globalInvitationRepository,
			CompanyMembershipRepository companyMembershipRepository) {
		this.companyRepository = companyRepository;
		this.companyLocationRepository = companyLocationRepository;
		this.userRepository = userRepository;
		this.slugGenerator = slugGenerator;
		this.deliveryLocationUpdateRepository = deliveryLocationUpdateRepository;
		this.deliverySessionRepository = deliverySessionRepository;
		this.stockBroadcastItemRepository = stockBroadcastItemRepository;
		this.stockBroadcastRepository = stockBroadcastRepository;
		this.orderItemRepository = orderItemRepository;
		this.orderRepository = orderRepository;
		this.menuPublicLinkRepository = menuPublicLinkRepository;
		this.menuItemRepository = menuItemRepository;
		this.menuRepository = menuRepository;
		this.invitationRepository = invitationRepository;
		this.globalInvitationRepository = globalInvitationRepository;
		this.companyMembershipRepository = companyMembershipRepository;
	}

	public List<CompanyResponse> list(CurrentUser currentUser) {
		requireCook(currentUser);
		return companyRepository.findByCookIdOrderByName(currentUser.userId()).stream().map(this::toResponse).toList();
	}

	public CompanyResponse get(CurrentUser currentUser, UUID id) {
		return toResponse(requireOwnedCompany(currentUser, id));
	}

	@Transactional
	public CompanyResponse create(CurrentUser currentUser, CompanyRequest request) {
		requireCook(currentUser);
		User cook = userRepository.findById(currentUser.userId()).orElseThrow(() -> ApiException.unauthorized("User not found"));
		Company company = new Company(cook, request.name().trim(), uniqueSlug(request.name()));
		apply(company, request);
		Company saved = companyRepository.save(company);
		companyLocationRepository.save(new CompanyLocation(saved));
		return toResponse(saved);
	}

	private String uniqueSlug(String name) {
		String base = slugGenerator.slugify(name);
		String slug = base;
		int suffix = 2;
		while (companyRepository.existsBySlug(slug)) {
			slug = base + "-" + suffix++;
		}
		return slug;
	}

	@Transactional
	public CompanyResponse update(CurrentUser currentUser, UUID id, CompanyRequest request) {
		Company company = requireOwnedCompany(currentUser, id);
		company.setName(request.name().trim());
		apply(company, request);
		company.setUpdatedAt(Instant.now());
		companyLocationRepository.save(new CompanyLocation(company));
		return toResponse(company);
	}

	@Transactional
	public CompanyResponse updateLocation(CurrentUser currentUser, UUID id, CompanyLocationRequest request) {
		Company company = requireOwnedCompany(currentUser, id);
		company.setAddress(request.address());
		company.setLatitude(request.latitude());
		company.setLongitude(request.longitude());
		company.setLocationSource(request.locationSource() == null ? LocationSource.MANUAL : request.locationSource());
		company.setUpdatedAt(Instant.now());
		companyLocationRepository.save(new CompanyLocation(company));
		return toResponse(company);
	}

	/**
	 * Elimina una empresa y todos sus datos relacionados en el orden correcto de FK.
	 * Las entidades sin cascade JPA se borran con queries bulk; los hijos de entidades
	 * con CascadeType.ALL se borran explícitamente antes del padre para evitar
	 * que la caché de primer nivel quede sucia al mezclar bulk-delete con JPA.
	 */
	@Transactional
	public void delete(CurrentUser currentUser, UUID id) {
		requireOwnedCompany(currentUser, id); // valida ownership

		// 1. delivery_location_updates (FK -> delivery_sessions -> company)
		deliveryLocationUpdateRepository.deleteByDeliverySessionCompanyId(id);

		// 2. delivery_sessions
		deliverySessionRepository.deleteByCompanyId(id);

		// 3. stock_broadcast_items (FK -> stock_broadcasts -> company)
		stockBroadcastItemRepository.deleteByStockBroadcastCompanyId(id);

		// 4. stock_broadcasts
		stockBroadcastRepository.deleteByCompanyId(id);

		// 5. order_items (FK -> orders -> company)
		orderItemRepository.deleteByOrderCompanyId(id);

		// 6. orders
		orderRepository.deleteByCompanyId(id);

		// 7. menu_public_links (FK directa a company O a menu de la company)
		menuPublicLinkRepository.deleteByCompanyIdOrMenuCompanyId(id);

		// 8. menu_item_companies join table (company como destino en GLOBAL menus)
		menuItemRepository.removeCompanyFromAllMenuItems(id);

		// 9. menu_companies join table (company asignada a GLOBAL menus)
		menuRepository.removeCompanyFromAllGlobalMenus(id);

		// 10. menu_items de los menúes COMPANY scope de esta empresa
		menuItemRepository.deleteByMenuCompanyId(id);

		// 11. menus COMPANY scope
		menuRepository.deleteByCompanyId(id);

		// 12. invitations individuales
		invitationRepository.deleteByCompanyId(id);

		// 13. global invitation
		globalInvitationRepository.deleteByCompanyId(id);

		// 14. membresías de empleados
		companyMembershipRepository.deleteByCompanyId(id);

		// 15. historial de ubicaciones
		companyLocationRepository.deleteByCompanyId(id);

		// 16. la empresa
		companyRepository.deleteById(id);
	}

	public Company requireOwnedCompany(CurrentUser currentUser, UUID id) {
		requireCook(currentUser);
		return companyRepository.findByIdAndCookId(id, currentUser.userId())
				.orElseThrow(() -> ApiException.notFound("Company not found"));
	}

	private void apply(Company company, CompanyRequest request) {
		company.setAddress(request.address());
		company.setNotes(request.notes());
		company.setLatitude(request.latitude());
		company.setLongitude(request.longitude());
		company.setLocationSource(request.locationSource());
		company.setWhatsappGroupLabel(request.whatsappGroupLabel());
	}

	private static void requireCook(CurrentUser currentUser) {
		if (!currentUser.isCook()) {
			throw ApiException.forbidden("Cook role required");
		}
	}

	private CompanyResponse toResponse(Company company) {
		return new CompanyResponse(
				company.getId(),
				company.getName(),
				company.getSlug(),
				company.getAddress(),
				company.getNotes(),
				company.getLatitude(),
				company.getLongitude(),
				company.getLocationSource(),
				company.getWhatsappGroupLabel());
	}

}
