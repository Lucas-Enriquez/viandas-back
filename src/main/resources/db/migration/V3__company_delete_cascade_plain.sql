-- Plain-SQL fallback: no funciones, no DO blocks. Para cada FK posible nombre,
-- DROP IF EXISTS y luego ADD con ON DELETE CASCADE usando el nombre que Hibernate espera.

-- company_locations.company_id
ALTER TABLE company_locations DROP CONSTRAINT IF EXISTS fk_company_locations_company;
ALTER TABLE company_locations DROP CONSTRAINT IF EXISTS company_locations_company_id_fkey;
ALTER TABLE company_locations ADD CONSTRAINT fk_company_locations_company
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE;

-- company_memberships.company_id
ALTER TABLE company_memberships DROP CONSTRAINT IF EXISTS fk_company_memberships_company;
ALTER TABLE company_memberships DROP CONSTRAINT IF EXISTS company_memberships_company_id_fkey;
ALTER TABLE company_memberships ADD CONSTRAINT fk_company_memberships_company
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE;

-- invitations.company_id
ALTER TABLE invitations DROP CONSTRAINT IF EXISTS fk_invitations_company;
ALTER TABLE invitations DROP CONSTRAINT IF EXISTS invitations_company_id_fkey;
ALTER TABLE invitations ADD CONSTRAINT fk_invitations_company
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE;

-- global_invitation.company_id
ALTER TABLE global_invitation DROP CONSTRAINT IF EXISTS fk_global_invitation_company;
ALTER TABLE global_invitation DROP CONSTRAINT IF EXISTS global_invitation_company_id_fkey;
ALTER TABLE global_invitation ADD CONSTRAINT fk_global_invitation_company
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE;

-- orders.company_id
ALTER TABLE orders DROP CONSTRAINT IF EXISTS fk_orders_company;
ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_company_id_fkey;
ALTER TABLE orders ADD CONSTRAINT fk_orders_company
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE;

-- stock_broadcasts.company_id
ALTER TABLE stock_broadcasts DROP CONSTRAINT IF EXISTS fk_stock_broadcasts_company;
ALTER TABLE stock_broadcasts DROP CONSTRAINT IF EXISTS stock_broadcasts_company_id_fkey;
ALTER TABLE stock_broadcasts ADD CONSTRAINT fk_stock_broadcasts_company
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE;

-- delivery_sessions.company_id
ALTER TABLE delivery_sessions DROP CONSTRAINT IF EXISTS fk_delivery_sessions_company;
ALTER TABLE delivery_sessions DROP CONSTRAINT IF EXISTS delivery_sessions_company_id_fkey;
ALTER TABLE delivery_sessions ADD CONSTRAINT fk_delivery_sessions_company
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE;

-- menus.company_id
ALTER TABLE menus DROP CONSTRAINT IF EXISTS fk_menus_company;
ALTER TABLE menus DROP CONSTRAINT IF EXISTS menus_company_id_fkey;
ALTER TABLE menus ADD CONSTRAINT fk_menus_company
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE;

-- menu_public_links.company_id
ALTER TABLE menu_public_links DROP CONSTRAINT IF EXISTS fk_menu_public_links_company;
ALTER TABLE menu_public_links DROP CONSTRAINT IF EXISTS menu_public_links_company_id_fkey;
ALTER TABLE menu_public_links ADD CONSTRAINT fk_menu_public_links_company
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE;

-- menu_public_links.menu_id
ALTER TABLE menu_public_links DROP CONSTRAINT IF EXISTS fk_menu_public_links_menu;
ALTER TABLE menu_public_links DROP CONSTRAINT IF EXISTS menu_public_links_menu_id_fkey;
ALTER TABLE menu_public_links ADD CONSTRAINT fk_menu_public_links_menu
    FOREIGN KEY (menu_id) REFERENCES menus(id) ON DELETE CASCADE;

-- menu_companies.company_id
ALTER TABLE menu_companies DROP CONSTRAINT IF EXISTS fk_menu_companies_company;
ALTER TABLE menu_companies DROP CONSTRAINT IF EXISTS menu_companies_company_id_fkey;
ALTER TABLE menu_companies ADD CONSTRAINT fk_menu_companies_company
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE;

-- menu_companies.menu_id
ALTER TABLE menu_companies DROP CONSTRAINT IF EXISTS fk_menu_companies_menu;
ALTER TABLE menu_companies DROP CONSTRAINT IF EXISTS menu_companies_menu_id_fkey;
ALTER TABLE menu_companies ADD CONSTRAINT fk_menu_companies_menu
    FOREIGN KEY (menu_id) REFERENCES menus(id) ON DELETE CASCADE;

-- menu_item_companies.company_id
ALTER TABLE menu_item_companies DROP CONSTRAINT IF EXISTS fk_menu_item_companies_company;
ALTER TABLE menu_item_companies DROP CONSTRAINT IF EXISTS menu_item_companies_company_id_fkey;
ALTER TABLE menu_item_companies ADD CONSTRAINT fk_menu_item_companies_company
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE;

-- menu_item_companies.menu_item_id
ALTER TABLE menu_item_companies DROP CONSTRAINT IF EXISTS fk_menu_item_companies_menu_item;
ALTER TABLE menu_item_companies DROP CONSTRAINT IF EXISTS menu_item_companies_menu_item_id_fkey;
ALTER TABLE menu_item_companies ADD CONSTRAINT fk_menu_item_companies_menu_item
    FOREIGN KEY (menu_item_id) REFERENCES menu_items(id) ON DELETE CASCADE;

-- order_items.order_id
ALTER TABLE order_items DROP CONSTRAINT IF EXISTS fk_order_items_order;
ALTER TABLE order_items DROP CONSTRAINT IF EXISTS order_items_order_id_fkey;
ALTER TABLE order_items ADD CONSTRAINT fk_order_items_order
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE;

-- stock_broadcast_items.stock_broadcast_id
ALTER TABLE stock_broadcast_items DROP CONSTRAINT IF EXISTS fk_stock_broadcast_items_stock_broadcast;
ALTER TABLE stock_broadcast_items DROP CONSTRAINT IF EXISTS stock_broadcast_items_stock_broadcast_id_fkey;
ALTER TABLE stock_broadcast_items ADD CONSTRAINT fk_stock_broadcast_items_stock_broadcast
    FOREIGN KEY (stock_broadcast_id) REFERENCES stock_broadcasts(id) ON DELETE CASCADE;

-- delivery_location_updates.delivery_session_id
ALTER TABLE delivery_location_updates DROP CONSTRAINT IF EXISTS fk_delivery_location_updates_delivery_session;
ALTER TABLE delivery_location_updates DROP CONSTRAINT IF EXISTS delivery_location_updates_delivery_session_id_fkey;
ALTER TABLE delivery_location_updates ADD CONSTRAINT fk_delivery_location_updates_delivery_session
    FOREIGN KEY (delivery_session_id) REFERENCES delivery_sessions(id) ON DELETE CASCADE;

-- menu_items.menu_id
ALTER TABLE menu_items DROP CONSTRAINT IF EXISTS fk_menu_items_menu;
ALTER TABLE menu_items DROP CONSTRAINT IF EXISTS menu_items_menu_id_fkey;
ALTER TABLE menu_items ADD CONSTRAINT fk_menu_items_menu
    FOREIGN KEY (menu_id) REFERENCES menus(id) ON DELETE CASCADE;
