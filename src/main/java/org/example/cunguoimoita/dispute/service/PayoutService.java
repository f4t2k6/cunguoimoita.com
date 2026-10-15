package org.example.cunguoimoita.dispute.service;

import org.example.cunguoimoita.common.db.ConnectionPool;
import org.example.cunguoimoita.dispute.dao.PayoutDAO;
import org.example.cunguoimoita.dispute.model.Payout;
import org.example.cunguoimoita.dispute.model.PayoutMethod;
import org.example.cunguoimoita.dispute.model.PayoutStatus;
import org.example.cunguoimoita.notification.model.NotificationType;
import org.example.cunguoimoita.notification.service.NotificationService;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Nghiệp vụ Phân hệ F9: Giải ngân tiền bán hàng (Payout), Tự động tính phí hoa hồng
 * theo Hạng thành viên (F8 MemberTier) và Xuất báo cáo đối soát CSV (Chương 18).
 */
public class PayoutService {

    private final PayoutDAO payoutDAO = new PayoutDAO();
    private final NotificationService notificationService = new NotificationService();

    /**
     * Tự động sinh Payout:
     * 1. Tra cứu MemberTier của seller để lấy discount_rate (F8).
     * 2. Phí sàn cơ bản: 5.0% (0.05).
     * 3. Tỷ lệ hoa hồng thực tế = 0.05 * (1.0 - discountRate).
     * 4. Tính commissionFee & netAmount.
     * 5. Tạo bản ghi Payout ở trạng thái 'scheduled'.
     */
    public Payout createPayout(String orderId, String sellerId, double grossAmount) throws SQLException {
        double discountRate = 0.0;
        String sqlTier = "SELECT mt.discount_rate FROM customers c "
                       + "JOIN member_tiers mt ON c.tier_id = mt.id "
                       + "WHERE c.user_id = ?";

        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(sqlTier)) {
                ps.setString(1, sellerId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        discountRate = rs.getDouble("discount_rate");
                    }
                }
            }

            double baseCommission = 0.05; // 5%
            double effectiveRate = baseCommission * (1.0 - discountRate);
            double commissionFee = Math.round(grossAmount * effectiveRate);
            double netAmount = Math.max(0, grossAmount - commissionFee);

            Payout p = new Payout();
            p.setId(UUID.randomUUID().toString());
            p.setOrderId(orderId);
            p.setSellerId(sellerId);
            p.setGrossAmount(grossAmount);
            p.setCommissionFee(commissionFee);
            p.setNetAmount(netAmount);
            p.setMethod(PayoutMethod.BANK_TRANSFER);
            p.setStatus(PayoutStatus.SCHEDULED);

            payoutDAO.insert(c, p);

            // Gửi thông báo cho Seller
            try {
                notificationService.send(sellerId, NotificationType.PAYMENT,
                        "Khoản giải ngân " + String.format("%,.0f", netAmount) + " đ cho đơn hàng đã được lên lịch.",
                        "/seller/payouts");
            } catch (Exception ignored) {
            }

            return p;
        }
    }

    /**
     * Xác nhận giải ngân thành công cho người bán.
     */
    public void processPayout(String payoutId) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            Payout p = payoutDAO.findById(c, payoutId);
            if (p != null) {
                payoutDAO.updateStatus(c, payoutId, PayoutStatus.DONE);
                try {
                    notificationService.send(p.getSellerId(), NotificationType.PAYMENT,
                            "Đã tất toán chuyển khoản " + String.format("%,.0f", p.getNetAmount()) + " đ vào tài khoản ngân hàng của bạn.",
                            "/seller/payouts");
                } catch (Exception ignored) {
                }
            }
        }
    }

    public List<Payout> getPayoutsBySeller(String sellerId) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            return payoutDAO.findBySellerId(c, sellerId);
        }
    }

    public PayoutDAO.SellerEarningsSummary getSellerSummary(String sellerId) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            return payoutDAO.getSellerSummary(c, sellerId);
        }
    }

    public Payout getByOrderId(String orderId) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            return payoutDAO.findByOrderId(c, orderId);
        }
    }

    /**
     * Xuất dữ liệu đối soát giải ngân ra định dạng CSV (Chương 18 kỹ thuật Servlet Response Header).
     */
    public void exportCsv(PrintWriter out, String sellerId) throws SQLException {
        out.write('\ufeff'); // UTF-8 BOM
        out.println("Mã Payout,Mã Đơn Hàng,Doanh Thu Gốc (VND),Phí Sàn (VND),Thực Nhận (VND),Hình Thức,Trạng Thái,Ngày Giải Ngân");

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        List<Payout> list = getPayoutsBySeller(sellerId);
        for (Payout p : list) {
            String dateStr = p.getPayoutDate() != null ? p.getPayoutDate().format(dtf) : "Đang chờ đối soát";
            out.printf("%s,%s,%.0f,%.0f,%.0f,%s,%s,%s%n",
                    p.getId(),
                    p.getOrderId(),
                    p.getGrossAmount(),
                    p.getCommissionFee(),
                    p.getNetAmount(),
                    p.getMethod().getDisplayName(),
                    p.getStatus().getDisplayName(),
                    dateStr
            );
        }
    }
}
