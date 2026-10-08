-- ====================================================================
-- MODULE F5: THANH TOÁN (PAYMENT)
-- Ánh xạ 100% từ Class Diagram: Cunguoimoita-Final-Ver-fixed
-- Phụ trách: Tuấn Anh (TV4)
-- Dự án: Cunguoimoita.com
-- ====================================================================

CREATE TABLE IF NOT EXISTS payments (
    id VARCHAR(36) PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,                       -- Mã thanh toán (code)
    order_id VARCHAR(36) NOT NULL,                          -- Đơn hàng (orderId)
    payer_id VARCHAR(36) NOT NULL,                          -- Người trả tiền (payerId)
    amount DECIMAL(12, 2) NOT NULL CHECK (amount >= 0),
    gateway VARCHAR(20) NOT NULL CHECK (gateway IN ('vnpay', 'momo', 'bank_transfer')),
    gateway_txn_id VARCHAR(100),                            -- Mã giao dịch cổng (gatewayTxnId)
    status VARCHAR(20) NOT NULL DEFAULT 'pending' 
        CHECK (status IN ('pending', 'paid', 'failed', 'refunded')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    paid_at TIMESTAMP,
    
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE RESTRICT,
    CONSTRAINT fk_payments_payer FOREIGN KEY (payer_id) REFERENCES users(id) ON DELETE RESTRICT
);

-- Chỉ mục tối ưu tốc độ tìm kiếm giao dịch
CREATE INDEX IF NOT EXISTS idx_payments_order_id ON payments(order_id);
CREATE INDEX IF NOT EXISTS idx_payments_status ON payments(status);
