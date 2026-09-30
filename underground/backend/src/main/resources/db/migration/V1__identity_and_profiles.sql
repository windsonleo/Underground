CREATE TABLE identity_accounts (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(10) NOT NULL CHECK (role IN ('CLIENT', 'HOST', 'ADMIN')),
    adult_confirmed BOOLEAN NOT NULL CHECK (adult_confirmed = TRUE),
    verification_status VARCHAR(12) NOT NULL CHECK (verification_status IN ('PENDING', 'VERIFIED', 'REJECTED')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
-- IDs cross module boundaries, never joins or foreign keys to private tables.
CREATE TABLE profiles (
    account_id VARCHAR(36) PRIMARY KEY,
    display_name VARCHAR(80) NOT NULL,
    bio VARCHAR(1000) NOT NULL
);
