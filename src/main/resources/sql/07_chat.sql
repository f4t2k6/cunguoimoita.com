-- F7. Tin nhắn, trả giá & thông báo (TV5) - PostgreSQL
-- Bảng theo class Conversation, Message, PriceOffer, Notification trong class diagram.
-- product_id, asker_id, seller_id, user_id trỏ tới bảng của TV1/TV2; khoá ngoại thêm sau khi các bảng đó có.

CREATE TABLE IF NOT EXISTS conversations (
    id              VARCHAR(36) PRIMARY KEY,
    product_id      VARCHAR(36) NOT NULL,
    asker_id        VARCHAR(36) NOT NULL,   -- người mua quan tâm, đang hỏi
    seller_id       VARCHAR(36) NOT NULL,   -- người đăng tin
    status          VARCHAR(10) NOT NULL DEFAULT 'open' CHECK (status IN ('open', 'closed')),
    created_at      TIMESTAMP   NOT NULL,
    last_message_at TIMESTAMP,
    UNIQUE (product_id, asker_id)            -- mỗi người hỏi 1 cuộc trò chuyện cho 1 bài
);
-- Hộp thư và số tin chưa đọc tìm theo người hỏi / người bán
CREATE INDEX IF NOT EXISTS idx_conversations_asker ON conversations (asker_id);
CREATE INDEX IF NOT EXISTS idx_conversations_seller ON conversations (seller_id);

CREATE TABLE IF NOT EXISTS messages (
    id              VARCHAR(36) PRIMARY KEY,
    conversation_id VARCHAR(36) NOT NULL REFERENCES conversations(id),
    sender_id       VARCHAR(36) NOT NULL,
    type            VARCHAR(20) NOT NULL CHECK (type IN ('text', 'image', 'price_offer')),
    content         TEXT        NOT NULL,   -- chữ, đường dẫn ảnh, hoặc id của PriceOffer
    sent_at         TIMESTAMP   NOT NULL,
    is_read         BOOLEAN     NOT NULL DEFAULT FALSE
);
CREATE INDEX IF NOT EXISTS idx_messages_conversation ON messages (conversation_id, sent_at);
-- Chỉ chứa tin chưa đọc nên rất nhỏ: đếm số trên mục Tin nhắn nhanh
CREATE INDEX IF NOT EXISTS idx_messages_unread ON messages (conversation_id) WHERE NOT is_read;

CREATE TABLE IF NOT EXISTS price_offers (
    id              VARCHAR(36)   PRIMARY KEY,
    conversation_id VARCHAR(36)   NOT NULL REFERENCES conversations(id),
    proposer_id     VARCHAR(36)   NOT NULL,  -- người đưa ra giá
    amount          NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    status          VARCHAR(10)   NOT NULL DEFAULT 'pending'
                    CHECK (status IN ('pending', 'accepted', 'declined', 'expired')),
    created_at      TIMESTAMP     NOT NULL,
    expires_at      TIMESTAMP     NOT NULL
);
-- Mỗi Conversation chỉ có 1 PriceOffer pending
CREATE UNIQUE INDEX IF NOT EXISTS uq_one_pending_offer ON price_offers (conversation_id) WHERE status = 'pending';
CREATE INDEX IF NOT EXISTS idx_price_offers_conversation ON price_offers (conversation_id);

CREATE TABLE IF NOT EXISTS notifications (
    id           VARCHAR(36) PRIMARY KEY,
    user_id      VARCHAR(36) NOT NULL,
    type         VARCHAR(10) NOT NULL CHECK (type IN ('order', 'payment', 'shipment', 'review', 'system')),
    content      TEXT        NOT NULL,
    link_url     VARCHAR(255),
    is_read      BOOLEAN     NOT NULL DEFAULT FALSE,
    created_date TIMESTAMP   NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_notifications_user ON notifications (user_id, created_date DESC);
-- Số trên chuông thông báo: chỉ chứa thông báo chưa đọc
CREATE INDEX IF NOT EXISTS idx_notifications_unread ON notifications (user_id) WHERE NOT is_read;
