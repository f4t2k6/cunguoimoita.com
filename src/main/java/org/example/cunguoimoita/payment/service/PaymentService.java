package org.example.cunguoimoita.payment.service;

import org.example.cunguoimoita.common.db.ConnectionPool;
import org.example.cunguoimoita.notification.model.NotificationType;
import org.example.cunguoimoita.notification.service.NotificationService;
import org.example.cunguoimoita.order.dao.OrderDAO;
import org.example.cunguoimoita.order.model.Order;
import org.example.cunguoimoita.payment.dao.PaymentDAO;
import org.example.cunguoimoita.payment.model.Payment;
import org.example.cunguoimoita.payment.model.PaymentGateway;
import org.example.cunguoimoita.payment.model.PaymentStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Nghiệp vụ Phân hệ F5: Thanh toán Escrow và Kiểm soát điều kiện COD.
 * Luật chính:
 *  - Số tiền luôn lấy từ tổng đơn trong DB (không tin số gửi lên từ form).
 *  - Chỉ người mua của đơn được thanh toán, và chỉ khi đơn đang "chờ thanh toán".
 *  - Đơn chỉ chuyển "đã thanh toán" khi cổng báo thành công (VNPay có chữ ký đúng, hoặc trang giả lập sandbox
 *    do chính người mua bấm), và số tiền khớp tổng đơn lúc đó.
 */
public class PaymentService {

    /** Số lần từ chối nhận hàng COD (vận đơn bị hoàn) để bị khoá COD. */
    public static final int MAX_COD_REFUSALS = 2;

    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final OrderDAO orderDAO = new OrderDAO();
    private final NotificationService notificationService = new NotificationService();

    /**
     * Customer.canUseCod(): người mua còn dùng được COD không.
     * Điều kiện: số lần từ chối nhận hàng (vận đơn bị hoàn) dưới 2 lần.
     */
    public boolean canUseCod(String customerId) throws SQLException {
        if (customerId == null) return false;
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            return countRefusals(c, customerId) < MAX_COD_REFUSALS;
        }
    }

    /**
     * Lý do đơn này KHÔNG được trả COD, hoặc null nếu được:
     *  1. Người mua đã từ chối nhận hàng COD từ 2 lần trở lên.
     *  2. Có sản phẩm trong đơn mà người bán tắt "nhận COD" (products.accept_cod = false).
     */
    public String codProblem(Connection c, String orderId, String buyerId) throws SQLException {
        if (countRefusals(c, buyerId) >= MAX_COD_REFUSALS) {
            return "Bạn đã từ chối nhận hàng COD " + MAX_COD_REFUSALS + " lần nên không dùng được COD, vui lòng thanh toán online.";
        }
        String sql = "SELECT 1 FROM order_items oi JOIN products p ON p.id = oi.product_id"
                   + " WHERE oi.order_id = ? AND NOT p.accept_cod";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return "Người bán không nhận thanh toán khi nhận hàng (COD) cho sản phẩm này.";
                }
            }
        }
        return null;
    }

    public String codProblem(String orderId, String buyerId) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            return codProblem(c, orderId, buyerId);
        }
    }

    /**
     * Payment.createPayment(): người mua bấm "Thanh toán" ở trang chọn cổng.
     * Đơn phải thuộc người mua và đang chờ thanh toán; số tiền = tổng đơn hiện tại.
     * Các lần thanh toán còn treo trước đó của đơn được huỷ để chỉ còn 1 lần đang chờ.
     */
    public Payment startPayment(String orderId, String buyerId, PaymentGateway gateway) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            c.setAutoCommit(false);
            try {
                Order order = orderDAO.findByIdForUpdate(c, orderId);
                if (order == null || !order.getBuyerId().equals(buyerId)) {
                    throw new SecurityException("Bạn không có quyền thanh toán đơn hàng này");
                }
                if (order.getStatus() != Order.Status.AWAITING_PAYMENT) {
                    throw new IllegalStateException("Đơn " + order.getCode() + " không ở trạng thái chờ thanh toán");
                }
                if (order.getTotalAmount() <= 0) {
                    throw new IllegalStateException("Tổng tiền đơn không hợp lệ");
                }
                paymentDAO.failPendingByOrderId(c, orderId);

                Payment p = new Payment();
                p.setId(UUID.randomUUID().toString());
                p.setCode("PAY-" + p.getId().substring(0, 8).toUpperCase());
                p.setOrderId(orderId);
                p.setPayerId(buyerId);
                p.setAmount(order.getTotalAmount());
                p.setGateway(gateway);
                p.setStatus(PaymentStatus.PENDING);
                p.setCreatedAt(LocalDateTime.now());
                paymentDAO.insert(c, p);
                c.commit();
                return p;
            } catch (SQLException | RuntimeException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    /**
     * Payment.markPaid(gatewayTxnId): cổng báo thanh toán thành công.
     * Chỉ nhận khi khoản đang chờ, đơn vẫn đang chờ thanh toán và số tiền khớp tổng đơn
     * (người mua đổi voucher/phí ship sau khi tạo thanh toán thì phải thanh toán lại).
     * Thành công → đơn chuyển paid, báo người mua và người bán.
     * @return true nếu khoản này đã được ghi nhận là đã thanh toán
     */
    public boolean markPaid(String paymentId, String gatewayTxnId) throws SQLException {
        Order paidOrder;
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            c.setAutoCommit(false);
            try {
                Payment p = paymentDAO.findByIdForUpdate(c, paymentId);
                if (p == null) {
                    c.rollback();
                    return false;
                }
                if (p.getStatus() == PaymentStatus.PAID) {
                    c.rollback();
                    return true;   // đã xử lý trước đó (VNPay có thể gọi lại nhiều lần)
                }
                Order order = orderDAO.findByIdForUpdate(c, p.getOrderId());
                boolean valid = p.getStatus() == PaymentStatus.PENDING
                        && order != null
                        && order.getStatus() == Order.Status.AWAITING_PAYMENT
                        && Math.abs(order.getTotalAmount() - p.getAmount()) < 0.5;
                if (!valid) {
                    paymentDAO.updateStatus(c, paymentId, PaymentStatus.FAILED, gatewayTxnId);
                    c.commit();
                    return false;
                }
                paymentDAO.updateStatus(c, paymentId, PaymentStatus.PAID, gatewayTxnId);
                order.setStatus(Order.Status.PAID);
                orderDAO.update(c, order);
                c.commit();
                paidOrder = order;
            } catch (SQLException | RuntimeException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(true);
            }
        }

        String link = "/order/detail?id=" + paidOrder.getId();
        try {
            notificationService.send(paidOrder.getBuyerId(), NotificationType.PAYMENT,
                    "Bạn đã thanh toán thành công đơn hàng " + paidOrder.getCode() + ". Khoản tiền đang được sàn giữ hộ (Escrow).",
                    link);
            notificationService.send(paidOrder.getSellerId(), NotificationType.PAYMENT,
                    "Đơn hàng " + paidOrder.getCode() + " đã được thanh toán. Vui lòng đóng gói và giao hàng.",
                    link);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    /** Payment.markFailed(): cổng báo thất bại. Chỉ đổi khoản đang chờ. */
    public boolean markFailed(String paymentId) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            Payment p = paymentDAO.findById(c, paymentId);
            if (p != null && p.getStatus() == PaymentStatus.PENDING) {
                paymentDAO.updateStatus(c, paymentId, PaymentStatus.FAILED, null);
                return true;
            }
            return false;
        }
    }

    /**
     * Trang giả lập cổng (khi chưa có khoá VNPay/MoMo thật): người mua bấm "Thanh toán thành công" hoặc "Thất bại".
     * Chỉ người trả tiền của khoản đó được bấm, và chỉ khi khoản còn đang chờ.
     */
    public boolean simulate(String paymentId, String userId, boolean success) throws SQLException {
        Payment p = requirePayer(paymentId, userId);
        if (p.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException("Khoản thanh toán này đã được xử lý");
        }
        if (success) {
            return markPaid(paymentId, "SANDBOX-" + p.getCode());
        }
        markFailed(paymentId);
        return false;
    }

    /** Khoản thanh toán phải tồn tại và thuộc người đang đăng nhập. */
    public Payment requirePayer(String paymentId, String userId) throws SQLException {
        Payment p = paymentId == null ? null : getById(paymentId);
        if (p == null || !p.getPayerId().equals(userId)) {
            throw new SecurityException("Không tìm thấy khoản thanh toán của bạn");
        }
        return p;
    }

    public Payment getById(String id) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            return paymentDAO.findById(c, id);
        }
    }

    public Payment getByOrderId(String orderId) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            return paymentDAO.findByOrderId(c, orderId);
        }
    }

    /** Lịch sử thanh toán của đơn (trang chi tiết đơn). */
    public List<Payment> getHistory(String orderId) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            return paymentDAO.findAllByOrderId(c, orderId);
        }
    }

    public Payment getByCode(String code) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            return paymentDAO.findByCode(c, code);
        }
    }

    public List<Payment> getRecentPayments(String payerId, int limit) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            return paymentDAO.findByPayerId(c, payerId, limit);
        }
    }

    // Số đơn COD người mua từ chối nhận (vận đơn bị hoàn).
    private int countRefusals(Connection c, String customerId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM shipments s JOIN orders o ON s.order_id = o.id "
                   + "WHERE o.buyer_id = ? AND s.status = 'returned' AND s.cod_amount > 0";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }
}
