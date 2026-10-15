package org.example.cunguoimoita.dispute.service;

import org.example.cunguoimoita.common.db.ConnectionPool;
import org.example.cunguoimoita.dispute.dao.RefundDAO;
import org.example.cunguoimoita.dispute.model.Refund;
import org.example.cunguoimoita.dispute.model.RefundReason;
import org.example.cunguoimoita.dispute.model.RefundStatus;
import org.example.cunguoimoita.payment.dao.PaymentDAO;
import org.example.cunguoimoita.payment.model.Payment;
import org.example.cunguoimoita.payment.model.PaymentStatus;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Nghiệp vụ Phân hệ F9: Hoàn tiền cho Người mua (Refund).
 */
public class RefundService {

    private final RefundDAO refundDAO = new RefundDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();

    public Refund createRefund(String orderId, String paymentId, String buyerId, double amount, RefundReason reason) throws SQLException {
        Refund r = new Refund();
        r.setId(UUID.randomUUID().toString());
        r.setOrderId(orderId);
        r.setPaymentId(paymentId);
        r.setBuyerId(buyerId);
        r.setAmount(amount);
        r.setReason(reason);
        r.setStatus(RefundStatus.PENDING);
        r.setRefundDate(LocalDateTime.now());

        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            refundDAO.insert(c, r);
        }
        return r;
    }

    public void processRefund(String refundId) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            c.setAutoCommit(false);
            try {
                Refund r = refundDAO.findById(c, refundId);
                if (r != null) {
                    refundDAO.updateStatus(c, refundId, RefundStatus.DONE);
                    if (r.getPaymentId() != null) {
                        Payment p = paymentDAO.findById(c, r.getPaymentId());
                        if (p != null) {
                            paymentDAO.updateStatus(c, p.getId(), PaymentStatus.REFUNDED, p.getGatewayTxnId());
                        }
                    }
                }
                c.commit();
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    public Refund getByOrderId(String orderId) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            return refundDAO.findByOrderId(c, orderId);
        }
    }

    public List<Refund> getByBuyerId(String buyerId) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            return refundDAO.findByBuyerId(c, buyerId);
        }
    }
}
