-- Correct existing invalid rows before running this migration if the database is not empty.
ALTER TABLE products
    ADD CONSTRAINT chk_products_price_positive CHECK (price > 0);
