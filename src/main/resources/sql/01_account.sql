-- F1: Tai khoan, ho so khach hang va so dia chi (PostgreSQL)
-- Chay file nay sau khi tao database cunguoimoita.

CREATE TABLE IF NOT EXISTS users (
    id              VARCHAR(36) PRIMARY KEY,
    email           VARCHAR(254) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    user_role       VARCHAR(20) NOT NULL DEFAULT 'customer'
                    CHECK (user_role IN ('customer', 'admin')),
    status          VARCHAR(20) NOT NULL DEFAULT 'active'
                    CHECK (status IN ('active', 'banned')),
    full_name       VARCHAR(120) NOT NULL,
    phone           VARCHAR(30) NOT NULL,
    avatar_path     VARCHAR(255),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_users_email_lower
    ON users (LOWER(email));

-- Customer la ho so mo rong cua User; admin khong co dong trong bang nay.
CREATE TABLE IF NOT EXISTS customers (
    user_id             VARCHAR(36) PRIMARY KEY
                        REFERENCES users(id) ON DELETE CASCADE,
    tier_id             VARCHAR(36), -- FK tới member_tiers do F8 quản lý
    payout_account_ref  VARCHAR(255),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE customers ADD COLUMN IF NOT EXISTS tier_id VARCHAR(36);

CREATE TABLE IF NOT EXISTS addresses (
    id              VARCHAR(36) PRIMARY KEY,
    user_id         VARCHAR(36) NOT NULL
                    REFERENCES users(id) ON DELETE CASCADE,
    label           VARCHAR(60) NOT NULL DEFAULT 'Địa chỉ mới',
    recipient_name  VARCHAR(120) NOT NULL,
    phone           VARCHAR(30) NOT NULL,
    address_line    VARCHAR(255) NOT NULL,
    ward            VARCHAR(120),
    district        VARCHAR(120),
    province        VARCHAR(120) NOT NULL,
    is_default      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_addresses_user_id ON addresses(user_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_addresses_one_default_per_user
    ON addresses(user_id) WHERE is_default = TRUE;

-- Bao dam thao tac tu DB bang script sau nay cung khong tao nhieu admin mac dinh.
CREATE INDEX IF NOT EXISTS idx_users_role_status ON users(user_role, status);
