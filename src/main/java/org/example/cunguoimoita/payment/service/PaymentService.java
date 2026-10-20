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
import org.example.cunguoimoita.shipping.dao.ShipmentDAO;
import org.example.cunguoimoita.shipping.service.ShippingService;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Thanh toán: số tiền luôn lấy từ tổng đơn trong DB, chỉ người mua của đơn đang chờ thanh toán được trả.
 * Đơn chỉ thành paid khi cổng báo thành công (chữ ký VNPay đúng hoặc sandbox) và số tiền khớp.
 */
public class PaymentService {

    /** Số lần từ chối nhận hàng COD (vận đơn bị hoàn) để bị khoá COD. */
    public static final int MAX_COD_REFUSALS = 2;

    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final OrderDAO orderDAO = new OrderDAO();
    private final ShipmentDAO shipmentDAO = new ShipmentDAO();
    private final NotificationService notificationService = new NotificationService();
    private final ShippingService shippingService = new ShippingService();

    /** @return lý do đơn không được trả COD (từ chối nhận COD quá nhiều lần, hoặc có món không nhận COD), null nếu được */
    public String codProblem(Connection c, String orderId, String buyerId) throws SQLException {
        if (shipmentDAO.countCodRefusals(c, buyerId) >= MAX_COD_REFUSALS) {
            return "Bạn đã từ chối nhận hàng COD " + MAX_COD_REFUSALS + " lần nên không dùng được COD, vui lòng thanh toán online.";
        }
        if (orderDAO.hasItemWithoutCod(c, orderId)) {
            return "Người bán không nhận thanh toán khi nhận hàng (COD) cho sản phẩm này.";
        }
        return null;
    }

    public String codProblem(String orderId, String buyerId) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            return codProblem(c, orderId, buyerId);
        }
    }

    /** Huỷ các lần thanh toán còn treo của đơn để chỉ còn 1 lần đang chờ. */
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

    /** Chỉ nhận khi số tiền khớp tổng đơn hiện tại (đổi voucher/phí ship sau khi tạo thanh toán thì phải trả lại). */
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
                shippingService.assignTrackingCode(c, order.getId());   // người bán chỉ còn xác nhận bàn giao
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
                    "Đơn hàng " + paidOrder.getCode() + " đã được thanh toán, mã vận đơn đã được tạo. Vui lòng đóng gói và xác nhận cách giao hàng cho đơn vị vận chuyển.",
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

    /** Trang giả lập cổng: chỉ người trả tiền bấm được, khi khoản còn đang chờ. */
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
