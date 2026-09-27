CREATE TABLE app_info (
                          id BIGSERIAL PRIMARY KEY,
                          app_name VARCHAR(100) NOT NULL,
                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO app_info (app_name)
VALUES ('Coffee Shop POS');