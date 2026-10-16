CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_categories_name_not_blank
        CHECK (LENGTH(BTRIM(name)) > 0)
);

-- Enforce case-insensitive uniqueness and ignore surrounding whitespace.
-- The service also normalizes names before saving.
CREATE UNIQUE INDEX uk_categories_name_normalized
    ON categories (LOWER(BTRIM(name)));
