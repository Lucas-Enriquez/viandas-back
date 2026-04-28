CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    role VARCHAR(30) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE oauth_accounts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    provider VARCHAR(40) NOT NULL,
    provider_subject VARCHAR(255) NOT NULL,
    provider_email VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_oauth_provider_subject UNIQUE (provider, provider_subject)
);

CREATE TABLE companies (
    id BIGSERIAL PRIMARY KEY,
    cook_id BIGINT NOT NULL REFERENCES users(id),
    name VARCHAR(160) NOT NULL,
    slug VARCHAR(180) NOT NULL UNIQUE,
    address VARCHAR(255),
    notes TEXT,
    latitude NUMERIC(10, 7),
    longitude NUMERIC(10, 7),
    location_source VARCHAR(40),
    whatsapp_group_label VARCHAR(160),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE company_locations (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    address VARCHAR(255),
    latitude NUMERIC(10, 7),
    longitude NUMERIC(10, 7),
    location_source VARCHAR(40),
    changed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE company_memberships (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_company_user UNIQUE (company_id, user_id)
);

CREATE TABLE invitations (
    id UUID PRIMARY KEY,
    token UUID UNIQUE NOT NULL,
    email VARCHAR(255),
    company_id BIGINT NOT NULL REFERENCES companies(id),
    used BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE menus (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    menu_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL,
    order_closes_at TIME NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_company_menu_date UNIQUE (company_id, menu_date)
);

CREATE TABLE menu_items (
    id BIGSERIAL PRIMARY KEY,
    menu_id BIGINT NOT NULL REFERENCES menus(id),
    name VARCHAR(180) NOT NULL,
    price NUMERIC(12, 2) NOT NULL,
    category VARCHAR(40) NOT NULL,
    photo_url VARCHAR(500),
    remaining_stock INTEGER,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE menu_public_links (
    id BIGSERIAL PRIMARY KEY,
    menu_id BIGINT NOT NULL REFERENCES menus(id),
    company_id BIGINT NOT NULL REFERENCES companies(id),
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    menu_id BIGINT NOT NULL REFERENCES menus(id),
    company_id BIGINT NOT NULL REFERENCES companies(id),
    customer_id BIGINT REFERENCES users(id),
    employee_id BIGINT REFERENCES users(id),
    source VARCHAR(30) NOT NULL,
    status VARCHAR(40) NOT NULL,
    customer_name_snapshot VARCHAR(160) NOT NULL,
    total_amount NUMERIC(12, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id),
    menu_item_id BIGINT NOT NULL REFERENCES menu_items(id),
    item_name_snapshot VARCHAR(180) NOT NULL,
    unit_price_snapshot NUMERIC(12, 2) NOT NULL,
    quantity INTEGER NOT NULL,
    comment VARCHAR(500)
);

CREATE TABLE stock_broadcasts (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    menu_id BIGINT NOT NULL REFERENCES menus(id),
    sent_by BIGINT NOT NULL REFERENCES users(id),
    message VARCHAR(500),
    sent_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE stock_broadcast_items (
    id BIGSERIAL PRIMARY KEY,
    stock_broadcast_id BIGINT NOT NULL REFERENCES stock_broadcasts(id),
    menu_item_id BIGINT NOT NULL REFERENCES menu_items(id)
);

CREATE TABLE notification_devices (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    token VARCHAR(500) NOT NULL UNIQUE,
    platform VARCHAR(40) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE delivery_sessions (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    menu_id BIGINT NOT NULL REFERENCES menus(id),
    cook_id BIGINT NOT NULL REFERENCES users(id),
    status VARCHAR(40) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    finished_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_approx_latitude NUMERIC(10, 7),
    last_approx_longitude NUMERIC(10, 7),
    last_location_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE delivery_location_updates (
    id BIGSERIAL PRIMARY KEY,
    delivery_session_id BIGINT NOT NULL REFERENCES delivery_sessions(id),
    approx_latitude NUMERIC(10, 7) NOT NULL,
    approx_longitude NUMERIC(10, 7) NOT NULL,
    accuracy_meters NUMERIC(8, 2),
    public_signal VARCHAR(40) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_companies_cook ON companies(cook_id);
CREATE INDEX idx_menu_items_menu ON menu_items(menu_id);
CREATE INDEX idx_orders_company_created ON orders(company_id, created_at);
CREATE INDEX idx_orders_customer_menu ON orders(customer_id, menu_id);
CREATE INDEX idx_delivery_sessions_company_status ON delivery_sessions(company_id, status);
