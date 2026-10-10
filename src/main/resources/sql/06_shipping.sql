-- F6. Vận chuyển & theo dõi đơn (TV5) - PostgreSQL
-- Bảng theo class ShippingCarrier và Shipment trong class diagram.
-- shipments.order_id trỏ tới bảng orders của TV3 (04_order.sql); khoá ngoại để sau khi bảng orders có.

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
    order_id         VARCHAR(36)   NOT NULL UNIQUE,  -- mỗi đơn 1 lần giao
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
    -- , FOREIGN KEY (order_id) REFERENCES orders(id)
);

-- Dữ liệu mẫu
INSERT INTO shipping_carriers (id, name, logo_url, hotline, base_fee, estimated_days, is_active) VALUES
('ghn',     'GHN',          NULL, '1900636677', 22000, 2, TRUE),
('ghtk',    'GHTK',         NULL, '1900636886', 20000, 3, TRUE),
('ahamove', 'Ahamove',      NULL, '1900545411', 35000, 1, TRUE),
('viettel', 'Viettel Post', NULL, '1900866868', 18000, 4, TRUE)
ON CONFLICT (id) DO NOTHING;
