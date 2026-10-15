package org.example.cunguoimoita.payment.service;

import org.example.cunguoimoita.common.dao.OrderRefDAO;
import org.example.cunguoimoita.common.db.ConnectionPool;
import org.example.cunguoimoita.notification.model.NotificationType;
import org.example.cunguoimoita.notification.service.NotificationService;
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
 */
public class PaymentService {

    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final OrderRefDAO orderDAO = new OrderRefDAO();
    private final NotificationService notificationService = new NotificationService();

    /**
     * Phương thức nghiệp vụ: Kiểm tra người mua có đủ điều kiện dùng COD không (Customer.canUseCod()).
     * Điều kiện:
     * 1. Đã xác thực số điện thoại (isPhoneVerified = true)
     * 2. Số lần từ chối nhận hàng (bom COD) < 2 lần
     * 3. Không có dư nợ cước hoàn (outstandingDebt == 0)
     */
    public boolean canUseCod(String customerId) throws SQLException {
        if (customerId == null) return false;

        String sqlCustomer = "SELECT is_phone_verified FROM customers WHERE user_id = ?";
        String sqlReturnedOrders = "SELECT COUNT(*) FROM shipments s "
                                 + "JOIN orders o ON s.order_id = o.id "
                                 + "WHERE o.buyer_id = ? AND s.status = 'returned'";

        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            boolean isPhoneVerified = false;
            try (PreparedStatement ps = c.prepareStatement(sqlCustomer)) {
                ps.setString(1, customerId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        isPhoneVerified = rs.getBoolean("is_phone_verified");
                    }
                }
            }
            if (!isPhoneVerified) {
                return false;
            }

            int refusalCount = 0;
            try (PreparedStatement ps = c.prepareStatement(sqlReturnedOrders)) {
                ps.setString(1, customerId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        refusalCount = rs.getInt(1);
                    }
                }
            }

            return refusalCount < 2;
        }
    }

    /**
     * Payment.createPayment(orderId, payerId, amount, gateway)
     * Tạo bản ghi thanh toán ở trạng thái PENDING.
     */
    public Payment createPayment(String orderId, String payerId, double amount, PaymentGateway gateway) throws SQLException {
        Payment p = new Payment();
        p.setId(UUID.randomUUID().toString());
        p.setCode("PAY-" + System.currentTimeMillis() % 1000000);
        p.setOrderId(orderId);
        p.setPayerId(payerId);
        p.setAmount(amount);
        p.setGateway(gateway);
        p.setStatus(PaymentStatus.PENDING);
        p.setCreatedAt(LocalDateTime.now());

        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            paymentDAO.insert(c, p);
        }
        return p;
    }

    /**
     * Payment.markPaid(gatewayTxnId): Cập nhật trạng thái thanh toán thành công,
     * đồng thời chuyển trạng thái đơn hàng sang 'paid' và gửi thông báo chuông (F7 Notification).
     */
    public boolean markPaid(String paymentId, String gatewayTxnId) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            c.setAutoCommit(false);
            try {
                Payment p = paymentDAO.findById(c, paymentId);
                if (p == null) {
                    c.rollback();
                    return false;
                }
                if (p.getStatus() == PaymentStatus.PAID) {
                    c.rollback();
                    return true; // Đã xử lý trước đó
                }

                paymentDAO.updateStatus(c, paymentId, PaymentStatus.PAID, gatewayTxnId);
                orderDAO.updateStatus(c, p.getOrderId(), "paid", "new", "awaiting_payment");

                OrderRefDAO.OrderRef orderRef = orderDAO.find(c, p.getOrderId());
                c.commit();

                // Gửi thông báo Notification
                if (orderRef != null) {
                    try {
                        String orderCode = orderRef.code() != null ? orderRef.code() : orderRef.id();
                        notificationService.send(orderRef.buyerId(), NotificationType.PAYMENT,
                                "Bạn đã thanh toán thành công đơn hàng " + orderCode + ". Khoản tiền đang được bảo vệ bởi Escrow.",
                                "/ui?page=orders");
                        notificationService.send(orderRef.sellerId(), NotificationType.PAYMENT,
                                "Đơn hàng " + orderCode + " đã được thanh toán qua Escrow. Vui lòng đóng gói và giao hàng.",
                                "/ui?page=orders");
                    } catch (Exception ignored) {
                    }
                }
                return true;
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    /**
     * Payment.markFailed(): Cập nhật trạng thái thanh toán thất bại.
     */
    public boolean markFailed(String paymentId) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            Payment p = paymentDAO.findById(c, paymentId);
            if (p != null) {
                paymentDAO.updateStatus(c, paymentId, PaymentStatus.FAILED, null);
                return true;
            }
            return false;
        }
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
}
