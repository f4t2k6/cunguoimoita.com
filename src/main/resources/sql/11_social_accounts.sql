-- OAuth social identities for F1. Run once on the same Neon database as 00_database_chung.sql.
CREATE TABLE IF NOT EXISTS social_accounts (
    id                VARCHAR(36) PRIMARY KEY,
    provider          VARCHAR(16) NOT NULL CHECK (provider IN ('google', 'facebook')),
    provider_user_id  VARCHAR(255) NOT NULL,
    user_id           VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_social_provider_identity UNIQUE (provider, provider_user_id),
    CONSTRAINT uq_social_user_provider UNIQUE (user_id, provider)
);

CREATE INDEX IF NOT EXISTS idx_social_accounts_user_id ON social_accounts(user_id);
