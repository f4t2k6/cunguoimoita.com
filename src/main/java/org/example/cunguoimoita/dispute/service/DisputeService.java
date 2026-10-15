package org.example.cunguoimoita.dispute.service;

import org.example.cunguoimoita.common.dao.OrderRefDAO;
import org.example.cunguoimoita.common.db.ConnectionPool;
import org.example.cunguoimoita.dispute.dao.DisputeDAO;
import org.example.cunguoimoita.dispute.dao.RefundDAO;
import org.example.cunguoimoita.dispute.model.Dispute;
import org.example.cunguoimoita.dispute.model.DisputeReason;
import org.example.cunguoimoita.dispute.model.DisputeStatus;
import org.example.cunguoimoita.dispute.model.Refund;
import org.example.cunguoimoita.dispute.model.RefundReason;
import org.example.cunguoimoita.dispute.model.RefundStatus;
import org.example.cunguoimoita.notification.model.NotificationType;
import org.example.cunguoimoita.notification.service.NotificationService;
import org.example.cunguoimoita.payment.dao.PaymentDAO;
import org.example.cunguoimoita.payment.model.Payment;
import org.example.cunguoimoita.payment.model.PaymentStatus;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Nghiệp vụ Phân hệ F9: Xử lý Khiếu nại (Dispute), Thẩm định bằng chứng và Phán quyết Trọng tài.
 */
public class DisputeService {

    private final DisputeDAO disputeDAO = new DisputeDAO();
    private final RefundDAO refundDAO = new RefundDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final OrderRefDAO orderDAO = new OrderRefDAO();
    private final NotificationService notificationService = new NotificationService();
    private final PayoutService payoutService = new PayoutService();

    /**
     * Dispute.openDispute(): Người mua mở yêu cầu khiếu nại đơn hàng.
     */
    public Dispute openDispute(String orderId, String raisedById, String againstId,
                               DisputeReason reason, String description, String evidenceImages) throws SQLException {
        Dispute d = new Dispute();
        d.setId(UUID.randomUUID().toString());
        d.setOrderId(orderId);
        d.setRaisedById(raisedById);
        d.setAgainstId(againstId);
        d.setReason(reason);
        d.setDescription(description);
        d.setEvidenceImages(evidenceImages);
        d.setStatus(DisputeStatus.OPEN);
        d.setCreatedDate(LocalDate.now());

        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            disputeDAO.insert(c, d);
        }

        // Báo cho bên bị khiếu nại và Admin
        try {
            notificationService.send(againstId, NotificationType.ORDER,
                    "Đơn hàng của bạn đang có khiếu nại (" + reason.getDisplayName() + "). Vui lòng nộp bằng chứng đối chiếu.",
                    "/dispute?id=" + d.getId());
        } catch (Exception ignored) {
        }
        return d;
    }

    /**
     * Dispute.submitEvidence(): Nộp thêm hình ảnh / video bằng chứng.
     */
    public void submitEvidence(String disputeId, String newImages) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            Dispute d = disputeDAO.findById(c, disputeId);
            if (d != null) {
                d.submitEvidence(newImages);
                disputeDAO.update(c, d);
            }
        }
    }

    /**
     * Admin đưa tranh chấp vào trạng thái thẩm định.
     */
    public void reviewDispute(String disputeId, String adminId) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            Dispute d = disputeDAO.findById(c, disputeId);
            if (d != null) {
                d.review();
                d.setHandledByAdminId(adminId);
                disputeDAO.update(c, d);
            }
        }
    }

    /**
     * Dispute.resolveForBuyer(): Phán quyết Người Mua Thắng.
     * Tự động sinh bản ghi Refund, đổi trạng thái payment sang 'refunded' và cập nhật order sang 'refunded'.
     */
    public Refund resolveForBuyer(String disputeId, String adminId, String resolutionNote) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            c.setAutoCommit(false);
            try {
                Dispute d = disputeDAO.findById(c, disputeId);
                if (d == null) {
                    c.rollback();
                    return null;
                }

                d.resolveForBuyer(adminId, resolutionNote);
                disputeDAO.update(c, d);

                // Cập nhật trạng thái đơn hàng
                orderDAO.updateStatus(c, d.getOrderId(), "refunded", "paid", "shipping", "delivered", "new");

                // Tìm thanh toán gốc để hoàn
                Payment p = paymentDAO.findByOrderId(c, d.getOrderId());
                double refundAmount = p != null ? p.getAmount() : 0.0;
                String paymentId = p != null ? p.getId() : null;

                if (p != null) {
                    paymentDAO.updateStatus(c, p.getId(), PaymentStatus.REFUNDED, p.getGatewayTxnId());
                }

                // Sinh bản ghi Refund
                Refund r = new Refund();
                r.setId(UUID.randomUUID().toString());
                r.setOrderId(d.getOrderId());
                r.setPaymentId(paymentId);
                r.setBuyerId(d.getRaisedById());
                r.setAmount(refundAmount);
                r.setReason(RefundReason.DISPUTE_WON);
                r.setStatus(RefundStatus.DONE);
                r.setRefundDate(LocalDateTime.now());
                refundDAO.insert(c, r);

                c.commit();

                // Gửi thông báo
                try {
                    notificationService.send(d.getRaisedById(), NotificationType.PAYMENT,
                            "Khiếu nại đơn hàng của bạn đã được giải quyết: Hoàn tiền " + String.format("%,.0f", refundAmount) + " đ.",
                            "/dispute?id=" + d.getId());
                    notificationService.send(d.getAgainstId(), NotificationType.ORDER,
                            "Khiếu nại đơn hàng đã được phán quyết: Hoàn tiền cho người mua.",
                            "/dispute?id=" + d.getId());
                } catch (Exception ignored) {
                }

                return r;
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    /**
     * Dispute.resolveForSeller(): Phán quyết Người Bán Thắng.
     * Bác khiếu nại, tự động tạo lệnh Payout giải ngân tiền cho người bán.
     */
    public void resolveForSeller(String disputeId, String adminId, String resolutionNote) throws SQLException {
        Dispute d;
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            d = disputeDAO.findById(c, disputeId);
            if (d == null) return;

            d.resolveForSeller(adminId, resolutionNote);
            disputeDAO.update(c, d);
        }

        // Tạo lệnh Payout giải ngân cho Người bán (againstId hoặc sellerId)
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            Payment p = paymentDAO.findByOrderId(c, d.getOrderId());
            double grossAmount = p != null ? p.getAmount() : 0.0;
            if (grossAmount > 0) {
                payoutService.createPayout(d.getOrderId(), d.getAgainstId(), grossAmount);
            }
        }

        try {
            notificationService.send(d.getAgainstId(), NotificationType.PAYMENT,
                    "Khiếu nại được phán quyết có lợi cho bạn. Lệnh giải ngân tiền bán hàng đã được tạo.",
                    "/seller/payouts");
            notificationService.send(d.getRaisedById(), NotificationType.ORDER,
                    "Khiếu nại đơn hàng của bạn không được chấp thuận bởi Trọng tài.",
                    "/dispute?id=" + d.getId());
        } catch (Exception ignored) {
        }
    }

    /**
     * Từ chối khiếu nại.
     */
    public void rejectDispute(String disputeId, String adminId, String resolutionNote) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            Dispute d = disputeDAO.findById(c, disputeId);
            if (d != null) {
                d.reject(adminId, resolutionNote);
                disputeDAO.update(c, d);
            }
        }
    }

    public Dispute getById(String id) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            return disputeDAO.findById(c, id);
        }
    }

    public Dispute getByOrderId(String orderId) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            return disputeDAO.findByOrderId(c, orderId);
        }
    }

    public List<Dispute> getByUser(String userId) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            return disputeDAO.findByUser(c, userId);
        }
    }

    public List<Dispute> listPendingForAdmin() throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            return disputeDAO.listPendingForAdmin(c);
        }
    }

    public List<Dispute> listAll(int limit) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            return disputeDAO.listAll(c, limit);
        }
    }
}
