-- ====================================================================
-- F8. ĐÁNH GIÁ & HẠNG THÀNH VIÊN (TV3 - Tuyết)
-- Bảng reviews, member_tiers đã có trong 00_database_chung.sql.
-- File này chỉ tạo VIEW tính các thuộc tính dẫn xuất của Customer:
--   /avgRating  = trung bình số sao người khác chấm cho mình
--   /totalSales = số đơn đã bán và hoàn tất
-- Trang hồ sơ công khai (F1 - PublicSellerProfileDAO) đọc view này.
-- Chạy lại nhiều lần được (CREATE OR REPLACE), không xoá dữ liệu.
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