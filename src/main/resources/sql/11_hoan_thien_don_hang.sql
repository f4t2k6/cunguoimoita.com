-- ====================================================================
-- 11. HOÀN THIỆN LUỒNG ĐƠN HÀNG (19/10/2026)
-- Chạy 1 lần trên DB chung (Neon) sau 00..10. Chạy lại nhiều lần cũng được, không xoá dữ liệu.
--  1. View seller_public_stats (trước chỉ có trong 08_review.sql): điểm TB, số đơn đã bán, hạng.
--     Trang Hạng thành viên và hồ sơ công khai cần view này.
--  2. disputes: thêm phần phản hồi của người bán (lời giải thích + ảnh bằng chứng).
--  3. Mỗi đơn chỉ có tối đa 1 khoản trả tiền người bán (payouts) và 1 khoản hoàn tiền (refunds),
--     để tự hoàn tất / bấm 2 lần không tạo trùng.
--  4. Voucher mẫu cho B1 để thử áp mã giảm giá.
-- ====================================================================

CREATE OR REPLACE VIEW seller_public_stats AS
SELECT cu.user_id,
       COALESCE((SELECT ROUND(AVG(r.rating), 1)
                 FROM reviews r
                 WHERE r.target_user_id = cu.user_id), 0)                     AS avg_rating,
       (SELECT COUNT(*)
        FROM orders o
        WHERE o.seller_id = cu.user_id AND o.status = 'completed')           AS total_sales,
       mt.name                                                               AS member_tier
FROM customers cu
         JOIN member_tiers mt ON mt.id = cu.tier_id;

ALTER TABLE disputes ADD COLUMN IF NOT EXISTS seller_response        TEXT;
ALTER TABLE disputes ADD COLUMN IF NOT EXISTS seller_evidence_images TEXT;
ALTER TABLE disputes ADD COLUMN IF NOT EXISTS seller_response_date   TIMESTAMP;

CREATE UNIQUE INDEX IF NOT EXISTS uq_payouts_order ON payouts (order_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_refunds_order ON refunds (order_id);

INSERT INTO vouchers (id, code, customer_id, discount_value, min_order_value, status, expiry_date) VALUES
('V-B1-50K', 'GIAM50K', 'B1', 50000, 500000, 'issued', CURRENT_DATE + 30),
('V-B1-OLD', 'HETHAN20K', 'B1', 20000, 0, 'issued', CURRENT_DATE - 1)
ON CONFLICT (id) DO NOTHING;
