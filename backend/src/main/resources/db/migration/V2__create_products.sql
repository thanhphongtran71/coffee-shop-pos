CREATE TABLE products (
                          id BIGSERIAL PRIMARY KEY,

                          sku VARCHAR(50) NOT NULL,
                          name VARCHAR(150) NOT NULL,
                          description VARCHAR(500),

                          price NUMERIC(19, 2) NOT NULL,

                          active BOOLEAN NOT NULL DEFAULT TRUE,

                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                          CONSTRAINT uk_products_sku UNIQUE (sku),
                          CONSTRAINT chk_products_price_non_negative CHECK (price >= 0)
);