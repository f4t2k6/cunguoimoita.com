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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/** Trả tiền người bán (Payout), phí hoa hồng theo hạng thành viên, xuất CSV đối soát. */
public class PayoutService {

    private final PayoutDAO payoutDAO = new PayoutDAO();
    private final NotificationService notificationService = new NotificationService();

    /** Phí sàn cơ bản 5% tiền hàng, được giảm theo ưu đãi hạng thành viên của người bán. */
    public static final double BASE_COMMISSION = 0.05;

    /**
     * Mỗi đơn 1 khoản; phí = tiền hàng × COMMISSION_RATE × (1 − discount_rate của hạng). Người bán chưa khai tài khoản
     * nhận tiền thì để scheduled, tác vụ định kỳ chuyển sau. Gọi notifyCreated(...) sau khi commit.
     */
    public Payout createForOrder(Connection c, String orderId, String sellerId, double grossAmount) throws SQLException {
        if (payoutDAO.findByOrderId(c, orderId) != null || grossAmount <= 0) {
            return null;
        }
        double discountRate = 0.0;
        String account = null;
        String sql = "SELECT mt.discount_rate, cu.payout_account_ref FROM customers cu "
                   + "JOIN member_tiers mt ON cu.tier_id = mt.id WHERE cu.user_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, sellerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    discountRate = rs.getDouble("discount_rate");
                    account = rs.getString("payout_account_ref");
                }
            }
        }

        double commissionFee = Math.round(grossAmount * BASE_COMMISSION * (1.0 - discountRate));
        boolean hasAccount = account != null && !account.isBlank();

        Payout p = new Payout();
        p.setId(UUID.randomUUID().toString());
        p.setOrderId(orderId);
        p.setSellerId(sellerId);
        p.setGrossAmount(grossAmount);
        p.setCommissionFee(commissionFee);
        p.setNetAmount(Math.max(0, grossAmount - commissionFee));
        p.setMethod(isEwallet(account) ? PayoutMethod.EWALLET : PayoutMethod.BANK_TRANSFER);
        p.setStatus(hasAccount ? PayoutStatus.DONE : PayoutStatus.SCHEDULED);
        p.setPayoutDate(hasAccount ? LocalDateTime.now() : null);
        payoutDAO.insert(c, p);
        return p;
    }

    /** Báo người bán về khoản vừa tạo (gọi sau khi commit). */
    public void notifyCreated(Payout p) {
        if (p == null) {
            return;
        }
        String money = String.format("%,.0f", p.getNetAmount());
        String content = p.getStatus() == PayoutStatus.DONE
                ? "Đã chuyển " + money + " đ tiền bán hàng (đã trừ phí sàn) vào tài khoản nhận tiền của bạn."
                : "Khoản " + money + " đ tiền bán hàng đang chờ chuyển. Hãy khai tài khoản nhận tiền trong Hồ sơ.";
        try {
            notificationService.send(p.getSellerId(), NotificationType.PAYMENT, content, "/seller/payouts");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Tác vụ định kỳ: chuyển các khoản đang chờ của người bán đã khai tài khoản. @return số khoản đã chuyển */
    public int processScheduled() throws SQLException {
        List<Payout> due;
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            due = payoutDAO.findScheduledWithAccount(c);
        }
        for (Payout p : due) {
            processPayout(p.getId());
        }
        return due.size();
    }

    // Tài khoản nhận tiền ghi kiểu "MoMo 09..." hay "Ví ..." thì coi là ví điện tử, còn lại là ngân hàng.
    private static boolean isEwallet(String account) {
        if (account == null) {
            return false;
        }
        String a = account.toLowerCase();
        return a.contains("momo") || a.contains("zalo") || a.contains("ví") || a.contains("vi dien tu");
    }

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

    /** Hạng, tỷ lệ phí thực tế và tài khoản nhận tiền của người bán (trang Thu nhập). */
    public record SellerInfo(String tierName, double commissionRate, String payoutAccount) {
    }

    public SellerInfo getSellerInfo(String sellerId) throws SQLException {
        String sql = "SELECT mt.name, mt.discount_rate, cu.payout_account_ref FROM customers cu "
                   + "JOIN member_tiers mt ON cu.tier_id = mt.id WHERE cu.user_id = ?";
        try (Connection c = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, sellerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new SellerInfo(rs.getString("name"),
                        BASE_COMMISSION * (1.0 - rs.getDouble("discount_rate")),
                        rs.getString("payout_account_ref"));
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

    public void exportCsv(PrintWriter out, String sellerId) throws SQLException {
        out.write('\ufeff');
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
