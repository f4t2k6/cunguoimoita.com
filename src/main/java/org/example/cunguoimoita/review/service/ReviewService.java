package org.example.cunguoimoita.review.service;

import org.example.cunguoimoita.admin.dao.MemberTierDAO;
import org.example.cunguoimoita.admin.model.MemberTier;
import org.example.cunguoimoita.common.db.ConnectionPool;
import org.example.cunguoimoita.notification.model.NotificationType;
import org.example.cunguoimoita.notification.service.NotificationService;
import org.example.cunguoimoita.order.dao.OrderDAO;
import org.example.cunguoimoita.order.model.Order;
import org.example.cunguoimoita.review.dao.ReviewDAO;
import org.example.cunguoimoita.review.dao.ReviewDAO.RatingSummary;
import org.example.cunguoimoita.review.dao.ReviewDAO.ReviewLine;
import org.example.cunguoimoita.review.model.Review;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

// Service của F8: đánh giá sau giao dịch + ecoPoints + hạng thành viên.
public class ReviewService {

    // Tiến độ hạng để hiện thanh %: điểm hiện có, hạng hiện tại, hạng kế tiếp (null nếu đã cao nhất).
    public record TierProgress(int points, MemberTier current, MemberTier next, int percent) {
    }

    private final ReviewDAO reviewDAO = new ReviewDAO();
    private final OrderDAO orderDAO = new OrderDAO();                  // đọc đơn (F4)
    private final MemberTierDAO tierDAO = new MemberTierDAO();         // danh sách hạng
    private final NotificationService notificationService = new NotificationService();

    // ===================== ĐÁNH GIÁ =====================

    // Customer.rateUser(targetId, rating): người mua chấm điểm người bán của 1 đơn đã hoàn tất.
    // Người được đánh giá tự suy ra từ đơn, không lấy từ form → không ai đánh giá bừa người ngoài đơn.
    public void rateUser(String orderId, String authorId, int rating, String content) throws SQLException {
        String targetId;
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            Order order = requireReviewable(c, orderId, authorId);
            targetId = authorId.equals(order.getBuyerId()) ? order.getSellerId() : order.getBuyerId();

            Review review = new Review();
            review.setOrderId(orderId);
            review.setAuthorId(authorId);
            review.setTargetUserId(targetId);
            review.setRating(rating);
            review.setContent(content);
            review.create();                     // Model kiểm tra 1–5 sao, độ dài nhận xét
            reviewDAO.insert(c, review);
        }
        notify(targetId, "Bạn vừa nhận được đánh giá " + rating + " sao", "/review/tier");
    }

    // Đánh giá người này đã viết cho đơn này (null nếu chưa có).
    public Review getMyReview(String orderId, String userId) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            return reviewDAO.findByOrderAndAuthor(c, orderId, userId);
        }
    }

    // Sửa đánh giá đã gửi: chỉ tác giả sửa được, cùng luật 1–5 sao và độ dài nhận xét như lúc tạo.
    public void updateReview(String orderId, String authorId, int rating, String content) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            Review review = reviewDAO.findByOrderAndAuthor(c, orderId, authorId);
            if (review == null) {
                throw new IllegalStateException("Bạn chưa đánh giá đơn này");
            }
            review.setRating(rating);
            review.setContent(content);
            review.create();                     // kiểm tra lại dữ liệu (không đổi ngày tạo trong DB)
            reviewDAO.update(c, review);
        }
    }

    // Đơn này người này còn đánh giá được không (dùng để hiện/ẩn nút "Đánh giá").
    public boolean canReview(String orderId, String userId) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            requireReviewable(c, orderId, userId);
            return true;
        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            return false;
        }
    }

    public List<ReviewLine> getReviewsOf(String userId) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            return reviewDAO.findByTarget(c, userId);
        }
    }

    // /avgRating, /totalSales, tên hạng.
    public RatingSummary getSummary(String userId) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            return reviewDAO.findSummary(c, userId);
        }
    }

    // ===================== ECO POINT & HẠNG =====================

    // Điểm + hạng hiện tại + hạng kế tiếp + % tiến độ, cho trang "Hạng thành viên".
    public TierProgress getTierProgress(String userId) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            int points = reviewDAO.ecoPoints(c, userId);
            List<MemberTier> tiers = tierDAO.findAll(c);       // đã sắp theo minEcoPoints tăng dần
            MemberTier current = tiers.get(0);
            MemberTier next = null;
            for (MemberTier tier : tiers) {
                if (points >= tier.getMinEcoPoints()) {
                    current = tier;                            // hạng cao nhất mà điểm đã đạt
                } else {
                    next = tier;                               // hạng đầu tiên chưa đạt
                    break;
                }
            }
            int percent = 100;
            if (next != null) {
                int range = next.getMinEcoPoints() - current.getMinEcoPoints();
                percent = (points - current.getMinEcoPoints()) * 100 / range;
            }
            return new TierProgress(points, current, next, percent);
        }
    }

    // Tính lại ecoPoints rồi xét hạng. OrderService.confirmReceived() gọi trong cùng transaction
    // ngay khi đơn chuyển COMPLETED, cho cả người mua và người bán.
    public void recalculateTier(Connection c, String userId) throws SQLException {
        int points = reviewDAO.ecoPoints(c, userId);
        MemberTier reached = null;
        for (MemberTier tier : tierDAO.findAll(c)) {
            if (points >= tier.getMinEcoPoints()) {
                reached = tier;
            }
        }
        if (reached != null) {
            reviewDAO.updateTier(c, userId, reached.getId());
        }
    }

    // ===================== HÀM PHỤ =====================

    // Luật đánh giá: đơn tồn tại, người này là người mua của đơn (người bán không đánh giá), đơn COMPLETED, chưa đánh giá đơn này.
    private Order requireReviewable(Connection c, String orderId, String userId) throws SQLException {
        Order order = orderDAO.findById(c, orderId);
        if (order == null) {
            throw new IllegalArgumentException("Không tìm thấy đơn hàng");
        }
        if (!userId.equals(order.getBuyerId()) && !userId.equals(order.getSellerId())) {
            throw new SecurityException("Bạn không thuộc đơn hàng này");
        }
        if (!userId.equals(order.getBuyerId())) {
            throw new SecurityException("Chỉ người mua mới đánh giá được giao dịch");
        }
        if (order.getStatus() != Order.Status.COMPLETED) {
            throw new IllegalStateException("Chỉ đánh giá được khi đơn đã hoàn tất");
        }
        if (reviewDAO.exists(c, orderId, userId)) {
            throw new IllegalStateException("Bạn đã đánh giá đơn này rồi");
        }
        return order;
    }

    private void notify(String userId, String content, String link) {
        try {
            notificationService.send(userId, NotificationType.REVIEW, content, link);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}