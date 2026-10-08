-- ====================================================================
-- MODULE F9: KHIẾU NẠI, HOÀN TIỀN & QUYẾT TOÁN (DISPUTE, REFUND, PAYOUT)
-- Ánh xạ 100% từ Class Diagram: Cunguoimoita-Final-Ver-fixed
-- Phụ trách: Tuấn Anh (TV4)
-- Dự án: Cunguoimoita.com
-- ====================================================================

-- 1. Bảng khiếu nại (Dispute)
CREATE TABLE IF NOT EXISTS disputes (
    id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    raised_by_id VARCHAR(36) NOT NULL,                     -- Người mở (raisedById)
    against_id VARCHAR(36) NOT NULL,                       -- Người bị khiếu nại (againstId)
    reason VARCHAR(50) NOT NULL 
        CHECK (reason IN ('not_received', 'wrong_item', 'damaged', 'fake', 'other')),
    description TEXT NOT NULL,
    evidence_images TEXT,                                  -- String[] lưu dạng JSON/chuỗi URL
    status VARCHAR(30) NOT NULL DEFAULT 'open' 
        CHECK (status IN ('open', 'reviewing', 'resolved_refund', 'resolved_payout', 'rejected')),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_date TIMESTAMP,
    handled_by_admin_id VARCHAR(36),                       -- Admin xử lý (handledByAdminId)
    resolution_note TEXT,                                  -- Ghi chú phán quyết (resolutionNote)
    
    CONSTRAINT fk_disputes_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE RESTRICT,
    CONSTRAINT fk_disputes_raised_by FOREIGN KEY (raised_by_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_disputes_against FOREIGN KEY (against_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_disputes_admin FOREIGN KEY (handled_by_admin_id) REFERENCES users(id) ON DELETE SET NULL
);

-- 2. Bảng hoàn tiền (Refund)
CREATE TABLE IF NOT EXISTS refunds (
    id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    payment_id VARCHAR(36),                                -- null nếu đơn là COD
    buyer_id VARCHAR(36) NOT NULL,                         -- Người mua nhận lại tiền (buyerId)
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

-- 3. Bảng quyết toán tiền người bán (Payout)
CREATE TABLE IF NOT EXISTS payouts (
    id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    seller_id VARCHAR(36) NOT NULL,                        -- Người bán nhận tiền (sellerId)
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

-- Chỉ mục tối ưu tốc độ truy vấn
CREATE INDEX IF NOT EXISTS idx_disputes_status ON disputes(status);
CREATE INDEX IF NOT EXISTS idx_payouts_seller_id ON payouts(seller_id);
