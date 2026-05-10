-- Asegura que cada columna afectada tenga UNA sola FK con ON DELETE CASCADE,
-- usando el nombre que Hibernate espera (fk_<tabla>_<columna_sin_sufijo_id>) para que
-- ddl-auto=update / validate no la recree. Es idempotente: dropea todas las FKs
-- existentes sobre la columna y agrega una con cascade.

CREATE OR REPLACE FUNCTION pg_temp.cascade_fk(
    p_table TEXT,
    p_column TEXT,
    p_ref_table TEXT
) RETURNS VOID AS $$
DECLARE
    cn RECORD;
    new_name TEXT;
BEGIN
    FOR cn IN
        SELECT c.conname
        FROM pg_constraint c
        WHERE c.conrelid = p_table::regclass
          AND c.contype = 'f'
          AND array_length(c.conkey, 1) = 1
          AND EXISTS (
              SELECT 1 FROM pg_attribute a
              WHERE a.attrelid = c.conrelid
                AND a.attnum = c.conkey[1]
                AND a.attname = p_column
          )
    LOOP
        EXECUTE format('ALTER TABLE %I DROP CONSTRAINT %I', p_table, cn.conname);
    END LOOP;

    new_name := 'fk_' || p_table || '_' || regexp_replace(p_column, '_id$', '');
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
