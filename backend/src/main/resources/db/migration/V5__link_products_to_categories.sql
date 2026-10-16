-- Step 1: Add the relationship as nullable first.
-- Existing products do not have category_id yet.
ALTER TABLE products
    ADD COLUMN category_id BIGINT;

-- Step 2: Create a default Category for products created before
-- Category Management existed.
-- ON CONFLICT handles a pre-existing Category with the same normalized name.
INSERT INTO categories (name, description, active)
VALUES (
    'Chưa phân loại',
    'Danh mục mặc định cho sản phẩm được tạo trước khi có Category Management.',
    TRUE
)
ON CONFLICT ( (LOWER(BTRIM(name))) ) DO NOTHING;

-- Step 3: Backfill every existing product before adding NOT NULL.
UPDATE products
SET category_id = (
    SELECT id
    FROM categories
    WHERE LOWER(BTRIM(name)) = LOWER(BTRIM('Chưa phân loại'))
)
WHERE category_id IS NULL;

-- Step 4: Enforce the required relationship.
ALTER TABLE products
    ALTER COLUMN category_id SET NOT NULL;

ALTER TABLE products
    ADD CONSTRAINT fk_products_category
    FOREIGN KEY (category_id)
    REFERENCES categories (id)
    ON DELETE RESTRICT;

-- Step 5: Support queries filtering products by category.
CREATE INDEX idx_products_category_id
    ON products (category_id);
