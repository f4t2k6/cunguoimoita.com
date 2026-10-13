-- ====================================================================
-- DATABASE CHUNG CỦA CẢ NHÓM - cunguoimoita.com (PostgreSQL)
-- Tạo đủ bảng cho 26 class trong class diagram (bản thầy sửa + PriceOffer),
-- kèm dữ liệu mẫu để chạy thử. Chạy MỘT LẦN trên database chung (Neon/Supabase/local).
--
-- Quy ước:
--   * tên bảng/cột dạng snake_case của thuộc tính trong diagram (passwordHash -> password_hash)
--   * id kiểu VARCHAR(36) (UUID dạng chuỗi), tiền NUMERIC(12,2)
--   * enum trong diagram -> VARCHAR + CHECK (...)
--   * String[] (images, evidenceImages) -> TEXT, lưu danh sách URL cách nhau bởi dấu phẩy hoặc JSON
--   * thuộc tính dẫn xuất (/ecoPoints, /avgRating, /totalSales, ...) KHÔNG lưu cột, tính bằng câu SELECT
--   * Guest không có bảng (khách chưa đăng nhập, chỉ có session)
--
-- Bảng của 05_payment.sql, 06_shipping.sql, 07_chat.sql, 09_dispute_payout.sql đã nằm trong file này
-- (cùng cột, cùng ràng buộc), nên trên database chung chỉ cần chạy file này.
--
-- Chạy lại từ đầu: bỏ comment khối DROP bên dưới (XOÁ HẾT DỮ LIỆU, báo nhóm trước khi chạy).
-- ====================================================================

-- DROP TABLE IF EXISTS payouts, refunds, disputes, payments, shipments, shipping_carriers,
--     reviews, order_items, orders, messages, price_offers, conversations, vouchers,
--     cart_items, carts, wishlist_products, wishlists, reports, products, categories,
--     notifications, addresses, customers, admins, member_tiers, users CASCADE;

-- ============================ F1. TÀI KHOẢN ============================

-- «abstract» User: phần chung của Admin và Customer (kế thừa -> mỗi lớp con 1 bảng, khoá chính = users.id)
CREATE TABLE IF NOT EXISTS users (
    id            VARCHAR(36)  PRIMARY KEY,
    fullname      VARCHAR(100) NOT NULL,
    email         VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    phone         VARCHAR(20),
    avatar_url    VARCHAR(255),
    join_date     DATE         NOT NULL DEFAULT CURRENT_DATE,
    status        VARCHAR(10)  NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'banned')),
    user_role     VARCHAR(10)  NOT NULL CHECK (user_role IN ('customer', 'admin'))
);

CREATE TABLE IF NOT EXISTS member_tiers (
    id             VARCHAR(36)  PRIMARY KEY,
    name           VARCHAR(10)  NOT NULL UNIQUE CHECK (name IN ('Bronze', 'Silver', 'Gold', 'Diamond')),
    min_eco_points INT          NOT NULL DEFAULT 0,
    discount_rate  NUMERIC(5,4) NOT NULL DEFAULT 0,   -- 0.03 = giảm 3%
    benefit_note   TEXT
);

CREATE TABLE IF NOT EXISTS admins (
    user_id          VARCHAR(36) PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    permission_level INT         NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS customers (
    user_id            VARCHAR(36)  PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    tier_id            VARCHAR(36)  NOT NULL REFERENCES member_tiers(id),   -- thuộc hạng (0..* -> 1)
    is_phone_verified  BOOLEAN      NOT NULL DEFAULT FALSE,
    payout_account_ref VARCHAR(100)
);

-- Address không có id trong diagram, nhưng Order.shippingAddressId trỏ tới nên cần khoá chính
CREATE TABLE IF NOT EXISTS addresses (
    id          VARCHAR(36)  PRIMARY KEY,
    customer_id VARCHAR(36)  NOT NULL REFERENCES customers(user_id) ON DELETE CASCADE,  -- sở hữu (1 -> 0..*)
    label       VARCHAR(50),
    street      VARCHAR(255) NOT NULL,
    ward        VARCHAR(100),
    district    VARCHAR(100),
    city        VARCHAR(100) NOT NULL,
    is_default  BOOLEAN      NOT NULL DEFAULT FALSE
);
-- mỗi khách chỉ 1 địa chỉ mặc định
CREATE UNIQUE INDEX IF NOT EXISTS uq_one_default_address ON addresses (customer_id) WHERE is_default;

-- User nhận Notification (1 -> 0..*)
CREATE TABLE IF NOT EXISTS notifications (
    id           VARCHAR(36) PRIMARY KEY,
    user_id      VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type         VARCHAR(10) NOT NULL CHECK (type IN ('order', 'payment', 'shipment', 'review', 'system')),
    content      TEXT        NOT NULL,
    link_url     VARCHAR(255),
    is_read      BOOLEAN     NOT NULL DEFAULT FALSE,
    created_date TIMESTAMP   NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_notifications_user ON notifications (user_id, created_date DESC);

-- ======================= F2/F3. SẢN PHẨM & FEED =======================

CREATE TABLE IF NOT EXISTS categories (
    id                 VARCHAR(36)  PRIMARY KEY,
    name               VARCHAR(100) NOT NULL,
    description        TEXT,
    parent_category_id VARCHAR(36)  REFERENCES categories(id)   -- là danh mục con của (0..* -> 0..1)
);

CREATE TABLE IF NOT EXISTS products (
    id                    VARCHAR(36)   PRIMARY KEY,
    seller_id             VARCHAR(36)   NOT NULL REFERENCES customers(user_id),  -- Customer đăng bán (1 -> 0..*)
    category_id           VARCHAR(36)   NOT NULL REFERENCES categories(id),      -- classifies (0..* -> 1)
    approved_by_admin_id  VARCHAR(36)   REFERENCES admins(user_id),              -- Admin duyệt (0..1)
    name                  VARCHAR(200)  NOT NULL,
    description           TEXT,
    images                TEXT,                                -- String[]: danh sách URL ảnh
    listing_type          VARCHAR(5)    NOT NULL CHECK (listing_type IN ('sale', 'swap', 'both')),
    price                 NUMERIC(12,2) CHECK (price >= 0),          -- giá thanh lý
    estimated_value       NUMERIC(12,2) CHECK (estimated_value >= 0), -- dùng khi listingType = swap
    desired_exchange_note TEXT,                                       -- dùng khi listingType = swap
    -- condition {mới, cũ, hỏng nhẹ}: lưu mã không dấu để dễ map sang enum Java
    condition             VARCHAR(10)   NOT NULL CHECK (condition IN ('new', 'used', 'damaged')),
    accept_cod            BOOLEAN       NOT NULL DEFAULT TRUE,
    verified_status       VARCHAR(20)   NOT NULL DEFAULT 'unverified'
                          CHECK (verified_status IN ('unverified', 'verified_by_partner', 'rejected')),
    status                VARCHAR(10)   NOT NULL DEFAULT 'draft'
                          CHECK (status IN ('draft', 'available', 'reserved', 'sold', 'exchanged', 'hidden')),
    posted_date           DATE          NOT NULL DEFAULT CURRENT_DATE
);
CREATE INDEX IF NOT EXISTS idx_products_category ON products (category_id, status);
CREATE INDEX IF NOT EXISTS idx_products_seller ON products (seller_id);

-- Wishlist (Bucket List): Customer sở hữu 0..1. Thuộc tính products: List<Product> lưu ở bảng phụ.
CREATE TABLE IF NOT EXISTS wishlists (
    id          VARCHAR(36) PRIMARY KEY,
    customer_id VARCHAR(36) NOT NULL UNIQUE REFERENCES customers(user_id) ON DELETE CASCADE,
    added_date  DATE        NOT NULL DEFAULT CURRENT_DATE
);
CREATE TABLE IF NOT EXISTS wishlist_products (
    wishlist_id VARCHAR(36) NOT NULL REFERENCES wishlists(id) ON DELETE CASCADE,
    product_id  VARCHAR(36) NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    PRIMARY KEY (wishlist_id, product_id)
);

-- Report: target là product hoặc user (target_type cho biết target_id trỏ bảng nào, nên không đặt khoá ngoại)
CREATE TABLE IF NOT EXISTS reports (
    id                  VARCHAR(36) PRIMARY KEY,
    reporter_id         VARCHAR(36) NOT NULL REFERENCES customers(user_id),   -- Customer gửi (1 -> 0..*)
    target_type         VARCHAR(10) NOT NULL CHECK (target_type IN ('product', 'user')),
    target_id           VARCHAR(36) NOT NULL,
    reason              VARCHAR(20) NOT NULL
                        CHECK (reason IN ('scam', 'fake_item', 'prohibited_item', 'spam', 'other')),
    description         TEXT,
    evidence_images     TEXT,
    status              VARCHAR(10) NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'resolved', 'rejected')),
    created_date        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    handled_by_admin_id VARCHAR(36) REFERENCES admins(user_id)                -- Admin xử lý (0..1)
);

-- ===================== F7. TIN NHẮN & TRẢ GIÁ (TV5) =====================

CREATE TABLE IF NOT EXISTS conversations (
    id              VARCHAR(36) PRIMARY KEY,
    product_id      VARCHAR(36) NOT NULL REFERENCES products(id),         -- hỏi về
    asker_id        VARCHAR(36) NOT NULL REFERENCES customers(user_id),   -- người mua quan tâm, đang hỏi
    seller_id       VARCHAR(36) NOT NULL REFERENCES customers(user_id),   -- người đăng tin
    status          VARCHAR(10) NOT NULL DEFAULT 'open' CHECK (status IN ('open', 'closed')),
    created_at      TIMESTAMP   NOT NULL,
    last_message_at TIMESTAMP,
    UNIQUE (product_id, asker_id)            -- mỗi người hỏi 1 cuộc trò chuyện cho 1 bài
);

CREATE TABLE IF NOT EXISTS messages (
    id              VARCHAR(36) PRIMARY KEY,
    conversation_id VARCHAR(36) NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,  -- composition
    sender_id       VARCHAR(36) NOT NULL REFERENCES users(id),
    type            VARCHAR(20) NOT NULL CHECK (type IN ('text', 'image', 'price_offer')),
    content         TEXT        NOT NULL,   -- chữ, đường dẫn ảnh, hoặc id của PriceOffer
    sent_at         TIMESTAMP   NOT NULL,
    is_read         BOOLEAN     NOT NULL DEFAULT FALSE
);
CREATE INDEX IF NOT EXISTS idx_messages_conversation ON messages (conversation_id, sent_at);

CREATE TABLE IF NOT EXISTS price_offers (
    id              VARCHAR(36)   PRIMARY KEY,
    conversation_id VARCHAR(36)   NOT NULL REFERENCES conversations(id),
    proposer_id     VARCHAR(36)   NOT NULL REFERENCES users(id),  -- người đưa ra giá
    amount          NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    status          VARCHAR(10)   NOT NULL DEFAULT 'pending'
                    CHECK (status IN ('pending', 'accepted', 'declined', 'expired')),
    created_at      TIMESTAMP     NOT NULL,
    expires_at      TIMESTAMP     NOT NULL
);
-- Mỗi Conversation chỉ có 1 PriceOffer pending
CREATE UNIQUE INDEX IF NOT EXISTS uq_one_pending_offer ON price_offers (conversation_id) WHERE status = 'pending';

-- ===================== F4. GIỎ HÀNG & ĐƠN HÀNG =====================

CREATE TABLE IF NOT EXISTS carts (
    id           VARCHAR(36) PRIMARY KEY,
    owner_id     VARCHAR(36) NOT NULL UNIQUE REFERENCES customers(user_id) ON DELETE CASCADE,  -- sở hữu (1 -> 1)
    created_date DATE        NOT NULL DEFAULT CURRENT_DATE
);

CREATE TABLE IF NOT EXISTS cart_items (
    id         VARCHAR(36) PRIMARY KEY,
    cart_id    VARCHAR(36) NOT NULL REFERENCES carts(id) ON DELETE CASCADE,   -- composition
    product_id VARCHAR(36) NOT NULL REFERENCES products(id),
    quantity   INT         NOT NULL DEFAULT 1 CHECK (quantity > 0),
    UNIQUE (cart_id, product_id)
);

CREATE TABLE IF NOT EXISTS vouchers (
    id              VARCHAR(36)   PRIMARY KEY,
    code            VARCHAR(50)   NOT NULL UNIQUE,
    customer_id     VARCHAR(36)   NOT NULL REFERENCES customers(user_id),   -- sở hữu (1 -> 0..*)
    discount_value  NUMERIC(12,2) NOT NULL CHECK (discount_value >= 0),
    min_order_value NUMERIC(12,2) NOT NULL DEFAULT 0,
    status          VARCHAR(10)   NOT NULL DEFAULT 'issued' CHECK (status IN ('issued', 'used', 'expired')),
    issued_date     DATE          NOT NULL DEFAULT CURRENT_DATE,
    expiry_date     DATE
);

CREATE TABLE IF NOT EXISTS orders (
    id                  VARCHAR(36)   PRIMARY KEY,
    code                VARCHAR(50)   NOT NULL UNIQUE,
    buyer_id            VARCHAR(36)   NOT NULL REFERENCES customers(user_id),   -- mua (buyer)
    seller_id           VARCHAR(36)   NOT NULL REFERENCES customers(user_id),   -- bán (seller)
    shipping_address_id VARCHAR(36)   NOT NULL REFERENCES addresses(id),        -- giao đến
    voucher_id          VARCHAR(36)   UNIQUE REFERENCES vouchers(id),           -- áp dụng (0..1 - 0..1)
    offer_id            VARCHAR(36)   UNIQUE REFERENCES price_offers(id),       -- null nếu mua qua giỏ hàng
    payment_method      VARCHAR(10)   NOT NULL CHECK (payment_method IN ('online', 'cod')),
    sub_total           NUMERIC(12,2) NOT NULL DEFAULT 0,
    shipping_fee        NUMERIC(12,2) NOT NULL DEFAULT 0,
    discount_amount     NUMERIC(12,2) NOT NULL DEFAULT 0,
    total_amount        NUMERIC(12,2) NOT NULL DEFAULT 0,
    status              VARCHAR(20)   NOT NULL DEFAULT 'new'
                        CHECK (status IN ('new', 'awaiting_payment', 'paid', 'shipping',
                                          'delivered', 'completed', 'cancelled', 'refunded')),
    order_date          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_date      TIMESTAMP,
    CHECK (buyer_id <> seller_id)
);
CREATE INDEX IF NOT EXISTS idx_orders_buyer ON orders (buyer_id);
CREATE INDEX IF NOT EXISTS idx_orders_seller ON orders (seller_id);

CREATE TABLE IF NOT EXISTS order_items (
    id         VARCHAR(36)   PRIMARY KEY,
    order_id   VARCHAR(36)   NOT NULL REFERENCES orders(id) ON DELETE CASCADE,   -- composition (1 -> 1..*)
    product_id VARCHAR(36)   NOT NULL REFERENCES products(id),
    quantity   INT           NOT NULL DEFAULT 1 CHECK (quantity > 0),
    unit_price NUMERIC(12,2) NOT NULL CHECK (unit_price >= 0)   -- giá chốt tại thời điểm mua
);

-- ===================== F8. ĐÁNH GIÁ =====================

CREATE TABLE IF NOT EXISTS reviews (
    id             VARCHAR(36) PRIMARY KEY,
    order_id       VARCHAR(36) NOT NULL REFERENCES orders(id),             -- 1 đơn tối đa 2 đánh giá
    author_id      VARCHAR(36) NOT NULL REFERENCES customers(user_id),     -- tác giả
    target_user_id VARCHAR(36) NOT NULL REFERENCES customers(user_id),     -- người được đánh giá
    rating         INT         NOT NULL CHECK (rating BETWEEN 1 AND 5),
    content        TEXT,
    created_date   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (order_id, author_id)        -- người mua và người bán mỗi người đánh giá 1 lần
);

-- ===================== F5. THANH TOÁN (TV4) =====================

CREATE TABLE IF NOT EXISTS payments (
    id VARCHAR(36) PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    order_id VARCHAR(36) NOT NULL,
    payer_id VARCHAR(36) NOT NULL,
    amount DECIMAL(12, 2) NOT NULL CHECK (amount >= 0),
    gateway VARCHAR(20) NOT NULL CHECK (gateway IN ('vnpay', 'momo', 'bank_transfer')),
    gateway_txn_id VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'pending'
        CHECK (status IN ('pending', 'paid', 'failed', 'refunded')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    paid_at TIMESTAMP,
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE RESTRICT,
    CONSTRAINT fk_payments_payer FOREIGN KEY (payer_id) REFERENCES users(id) ON DELETE RESTRICT
);
CREATE INDEX IF NOT EXISTS idx_payments_order_id ON payments(order_id);
CREATE INDEX IF NOT EXISTS idx_payments_status ON payments(status);

-- ===================== F6. VẬN CHUYỂN (TV5) =====================

CREATE TABLE IF NOT EXISTS shipping_carriers (
    id             VARCHAR(36)   PRIMARY KEY,
    name           VARCHAR(100)  NOT NULL,          -- GHN, GHTK, Ahamove, Viettel Post
    logo_url       VARCHAR(255),
    hotline        VARCHAR(20),
    base_fee       NUMERIC(12,2) NOT NULL,
    estimated_days INT           NOT NULL,
    is_active      BOOLEAN       NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS shipments (
    id               VARCHAR(36)   PRIMARY KEY,
    order_id         VARCHAR(36)   NOT NULL UNIQUE REFERENCES orders(id),  -- mỗi đơn 1 lần giao (1 -> 0..1)
    carrier_id       VARCHAR(36)   NOT NULL REFERENCES shipping_carriers(id),
    tracking_code    VARCHAR(50),
    shipping_fee     NUMERIC(12,2) NOT NULL,
    status           VARCHAR(20)   NOT NULL DEFAULT 'created'
                     CHECK (status IN ('created', 'picked_up', 'in_transit', 'delivered', 'returned')),
    created_date     DATE          NOT NULL,
    delivered_date   DATE,
    cod_amount       NUMERIC(12,2) NOT NULL DEFAULT 0, -- 0 nếu đơn đã trả trước online
    cod_status       VARCHAR(20)   NOT NULL DEFAULT 'none'
                     CHECK (cod_status IN ('none', 'pending', 'collected', 'remitted')),
    cod_collected_at TIMESTAMP,                        -- shipper thu tiền của người mua
    cod_remitted_at  TIMESTAMP                         -- shipper nộp tiền về sàn
);

-- ============ F9. KHIẾU NẠI, HOÀN TIỀN & GIẢI NGÂN (TV4) ============

CREATE TABLE IF NOT EXISTS disputes (
    id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    raised_by_id VARCHAR(36) NOT NULL,
    against_id VARCHAR(36) NOT NULL,
    reason VARCHAR(50) NOT NULL
        CHECK (reason IN ('not_received', 'wrong_item', 'damaged', 'fake', 'other')),
    description TEXT NOT NULL,
    evidence_images TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'open'
        CHECK (status IN ('open', 'reviewing', 'resolved_refund', 'resolved_payout', 'rejected')),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_date TIMESTAMP,
    handled_by_admin_id VARCHAR(36),
    resolution_note TEXT,
    CONSTRAINT fk_disputes_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE RESTRICT,
    CONSTRAINT fk_disputes_raised_by FOREIGN KEY (raised_by_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_disputes_against FOREIGN KEY (against_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_disputes_admin FOREIGN KEY (handled_by_admin_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS refunds (
    id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    payment_id VARCHAR(36),
    buyer_id VARCHAR(36) NOT NULL,
    amount DECIMAL(12, 2) NOT NULL CHECK (amount > 0),
    reason VARCHAR(50) NOT NULL
        CHECK (reason IN ('cancelled', 'not_received', 'wrong_item', 'dispute_won')),
    status VARCHAR(20) NOT NULL DEFAULT 'pending'
        CHECK (status IN ('pending', 'done', 'failed')),
    refund_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refunds_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE RESTRICT,
    CONSTRAINT fk_refunds_payment FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE SET NULL,
    CONSTRAINT fk_refunds_buyer FOREIGN KEY (buyer_id) REFERENCES users(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS payouts (
    id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    seller_id VARCHAR(36) NOT NULL,
    gross_amount DECIMAL(12, 2) NOT NULL CHECK (gross_amount >= 0),
    commission_fee DECIMAL(12, 2) NOT NULL DEFAULT 0.00 CHECK (commission_fee >= 0),
    net_amount DECIMAL(12, 2) NOT NULL CHECK (net_amount >= 0),
    method VARCHAR(30) NOT NULL CHECK (method IN ('bank_transfer', 'ewallet')),
    status VARCHAR(20) NOT NULL DEFAULT 'scheduled'
        CHECK (status IN ('scheduled', 'done', 'failed')),
    payout_date TIMESTAMP,
    CONSTRAINT fk_payouts_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE RESTRICT,
    CONSTRAINT fk_payouts_seller FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE RESTRICT
);
CREATE INDEX IF NOT EXISTS idx_disputes_status ON disputes(status);
CREATE INDEX IF NOT EXISTS idx_payouts_seller_id ON payouts(seller_id);

-- ====================================================================
-- DỮ LIỆU MẪU (chạy lại nhiều lần không bị trùng)
-- Đăng nhập tạm bằng ?as=<id>: S1, S2 là người bán; B1, B2 là người mua; A1 là admin.
-- password_hash mẫu chỉ là chuỗi giả, TV1 thay khi làm đăng nhập thật.
-- ====================================================================

-- discount_rate tạm đặt, nhóm chốt lại sau
INSERT INTO member_tiers (id, name, min_eco_points, discount_rate, benefit_note) VALUES
('bronze',  'Bronze',     0, 0.0000, 'Hạng mặc định'),
('silver',  'Silver',   500, 0.0100, 'Giảm 1% phí'),
('gold',    'Gold',    1000, 0.0300, 'Giảm 3% phí'),
('diamond', 'Diamond', 3000, 0.0500, 'Giảm 5% phí')
ON CONFLICT (id) DO NOTHING;

INSERT INTO users (id, fullname, email, password_hash, phone, user_role) VALUES
('A1', 'Quản trị viên', 'admin@cunguoimoita.com', 'CHUA_CO_HASH', '0900000000', 'admin'),
('S1', 'Mai Thành',     's1@cunguoimoita.com',    'CHUA_CO_HASH', '0900000001', 'customer'),
('S2', 'Huy Phát',      's2@cunguoimoita.com',    'CHUA_CO_HASH', '0900000002', 'customer'),
('B1', 'Người mua 1',   'b1@cunguoimoita.com',    'CHUA_CO_HASH', '0900000003', 'customer'),
('B2', 'Người mua 2',   'b2@cunguoimoita.com',    'CHUA_CO_HASH', '0900000004', 'customer')
ON CONFLICT (id) DO NOTHING;

INSERT INTO admins (user_id, permission_level) VALUES ('A1', 10) ON CONFLICT (user_id) DO NOTHING;

INSERT INTO customers (user_id, tier_id, is_phone_verified) VALUES
('S1', 'gold',   TRUE),
('S2', 'silver', TRUE),
('B1', 'bronze', FALSE),
('B2', 'bronze', FALSE)
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO addresses (id, customer_id, label, street, ward, district, city, is_default) VALUES
('AD-S1', 'S1', 'Nhà', '1 Võ Văn Ngân', 'Linh Chiểu', 'Thủ Đức', 'TP.HCM', TRUE),
('AD-S2', 'S2', 'Nhà', '12 Cầu Giấy', 'Quan Hoa', 'Cầu Giấy', 'Hà Nội', TRUE),
('AD-B1', 'B1', 'Nhà', '45 Lê Lợi', 'Bến Nghé', 'Quận 1', 'TP.HCM', TRUE),
('AD-B2', 'B2', 'Ký túc xá', 'KTX khu B', 'Đông Hoà', 'Dĩ An', 'Bình Dương', TRUE)
ON CONFLICT (id) DO NOTHING;

-- 9 danh mục giống thanh danh mục ở trang chủ (id = slug trong HomeServlet)
INSERT INTO categories (id, name, description, parent_category_id) VALUES
('dien-thoai',   'Điện thoại',         NULL, NULL),
('noxh',         'NOXH',               NULL, NULL),
('bat-dong-san', 'Bất động sản',       NULL, NULL),
('xe-co',        'Xe cộ',              NULL, NULL),
('viec-lam',     'Việc làm',           NULL, NULL),
('dien-tu',      'Điện tử',            NULL, NULL),
('thu-cung',     'Thú cưng',           NULL, NULL),
('dien-lanh',    'Điện lạnh',          NULL, NULL),
('noi-that',     'Nội thất, cây cảnh', NULL, NULL),
('ban-phim',     'Bàn phím, chuột',    NULL, 'dien-tu')
ON CONFLICT (id) DO NOTHING;

-- 2 bài đăng mẫu giống trang chủ (P1, P2)
INSERT INTO products (id, seller_id, category_id, approved_by_admin_id, name, description, images,
                      listing_type, price, estimated_value, desired_exchange_note, condition,
                      accept_cod, verified_status, status) VALUES
('P1', 'S1', 'ban-phim', 'A1', 'Chuột Atk Ghost Ultimate bạc',
 'Còn em Atk Ghost Ultimate màu bạc likenew fullbox đẹp 1m7', NULL,
 'sale', 1700000, NULL, NULL, 'new', TRUE, 'unverified', 'available'),
('P2', 'S2', 'ban-phim', 'A1', 'Bàn phím cơ 75%',
 'Pass lại bàn phím cơ 75%, switch đã lube, đổi được tai nghe', NULL,
 'both', 850000, 850000, 'Đổi tai nghe tương đương', 'used', TRUE, 'unverified', 'available')
ON CONFLICT (id) DO NOTHING;

INSERT INTO carts (id, owner_id) VALUES
('C-S1', 'S1'), ('C-S2', 'S2'), ('C-B1', 'B1'), ('C-B2', 'B2')
ON CONFLICT (id) DO NOTHING;

INSERT INTO wishlists (id, customer_id) VALUES ('W-B1', 'B1') ON CONFLICT (id) DO NOTHING;
INSERT INTO wishlist_products (wishlist_id, product_id) VALUES ('W-B1', 'P1') ON CONFLICT DO NOTHING;

INSERT INTO shipping_carriers (id, name, logo_url, hotline, base_fee, estimated_days, is_active) VALUES
('ghn',     'GHN',          NULL, '1900636677', 22000, 2, TRUE),
('ghtk',    'GHTK',         NULL, '1900636886', 20000, 3, TRUE),
('ahamove', 'Ahamove',      NULL, '1900545411', 35000, 1, TRUE),
('viettel', 'Viettel Post', NULL, '1900866868', 18000, 4, TRUE)
ON CONFLICT (id) DO NOTHING;

-- 1 đơn mẫu để thử vận chuyển, thanh toán, khiếu nại: B1 mua P2 của S2, trả COD
INSERT INTO orders (id, code, buyer_id, seller_id, shipping_address_id, payment_method,
                    sub_total, shipping_fee, discount_amount, total_amount, status) VALUES
('O1', 'ORD-0001', 'B1', 'S2', 'AD-B1', 'cod', 850000, 37000, 0, 887000, 'new')
ON CONFLICT (id) DO NOTHING;
INSERT INTO order_items (id, order_id, product_id, quantity, unit_price) VALUES
('OI1', 'O1', 'P2', 1, 850000)
ON CONFLICT (id) DO NOTHING;
