ALTER TABLE orders
    ADD COLUMN paid BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN paid_at TIMESTAMPTZ,
    ADD COLUMN payment_note VARCHAR(280);

CREATE INDEX idx_orders_paid ON orders(company_id, paid);
