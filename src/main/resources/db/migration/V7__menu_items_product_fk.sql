ALTER TABLE menu_items ADD COLUMN product_id UUID;

ALTER TABLE menu_items
    ADD CONSTRAINT fk_menu_items_product
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE SET NULL;

CREATE INDEX idx_menu_items_product ON menu_items(product_id);
