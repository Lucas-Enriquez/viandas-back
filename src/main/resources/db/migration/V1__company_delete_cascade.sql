-- Convierte las FKs que dependen (directa o transitivamente) de companies a ON DELETE CASCADE.
-- Permite borrar una empresa con un único DELETE FROM companies y deja Postgres cascadear todo.
-- Los nombres de constraint los genera Hibernate dinámicamente (FK<hash>), por eso se busca por
-- tabla + columna en pg_constraint en lugar de hardcodearlos.

CREATE OR REPLACE FUNCTION pg_temp.cascade_fk(
    p_table TEXT,
    p_column TEXT,
    p_ref_table TEXT
) RETURNS VOID AS $$
DECLARE
    old_name TEXT;
    new_name TEXT;
BEGIN
    SELECT c.conname INTO old_name
    FROM pg_constraint c
    JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = ANY(c.conkey)
    WHERE c.conrelid = p_table::regclass
      AND c.contype = 'f'
      AND a.attname = p_column
      AND array_length(c.conkey, 1) = 1
    LIMIT 1;

    IF old_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE %I DROP CONSTRAINT %I', p_table, old_name);
    END IF;

    new_name := p_table || '_' || p_column || '_fkey';
    EXECUTE format(
        'ALTER TABLE %I ADD CONSTRAINT %I FOREIGN KEY (%I) REFERENCES %I(id) ON DELETE CASCADE',
        p_table, new_name, p_column, p_ref_table
    );
END;
$$ LANGUAGE plpgsql;

-- Hijos directos de companies
SELECT pg_temp.cascade_fk('company_locations',     'company_id', 'companies');
SELECT pg_temp.cascade_fk('company_memberships',   'company_id', 'companies');
SELECT pg_temp.cascade_fk('invitations',           'company_id', 'companies');
SELECT pg_temp.cascade_fk('global_invitation',     'company_id', 'companies');
SELECT pg_temp.cascade_fk('orders',                'company_id', 'companies');
SELECT pg_temp.cascade_fk('stock_broadcasts',      'company_id', 'companies');
SELECT pg_temp.cascade_fk('delivery_sessions',     'company_id', 'companies');
SELECT pg_temp.cascade_fk('menus',                 'company_id', 'companies');
SELECT pg_temp.cascade_fk('menu_public_links',     'company_id', 'companies');

-- Join tables (cascade desde ambos lados)
SELECT pg_temp.cascade_fk('menu_companies',        'company_id', 'companies');
SELECT pg_temp.cascade_fk('menu_companies',        'menu_id',    'menus');
SELECT pg_temp.cascade_fk('menu_item_companies',   'company_id', 'companies');
SELECT pg_temp.cascade_fk('menu_item_companies',   'menu_item_id', 'menu_items');

-- Hijos transitivos (cascade vía padre)
SELECT pg_temp.cascade_fk('order_items',                'order_id',            'orders');
SELECT pg_temp.cascade_fk('stock_broadcast_items',      'stock_broadcast_id',  'stock_broadcasts');
SELECT pg_temp.cascade_fk('delivery_location_updates',  'delivery_session_id', 'delivery_sessions');
SELECT pg_temp.cascade_fk('menu_items',                 'menu_id',             'menus');
SELECT pg_temp.cascade_fk('menu_public_links',          'menu_id',             'menus');
