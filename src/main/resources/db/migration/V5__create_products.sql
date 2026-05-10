CREATE TABLE products (
    id UUID PRIMARY KEY,
    cook_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(180) NOT NULL,
    price NUMERIC(12, 2) NOT NULL,
    category VARCHAR(40) NOT NULL,
    photo_url VARCHAR(500),
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_products_cook_name UNIQUE (cook_id, name)
);

CREATE INDEX idx_products_cook ON products(cook_id);
CREATE INDEX idx_products_cook_category ON products(cook_id, category);
