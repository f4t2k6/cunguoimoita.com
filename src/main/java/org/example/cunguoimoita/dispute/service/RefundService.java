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

public class RefundService {

    private final RefundDAO refundDAO = new RefundDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();

    /**
     * Online hoàn đúng số đã trả; COD chỉ hoàn khi shipper đã thu. Hoàn tiền là giả lập nên ghi luôn done.
     * @return null nếu người mua chưa trả đồng nào hoặc đơn đã được hoàn
     */
    public Refund refundOrder(Connection c, String orderId, String buyerId, double codCollectedAmount,
                              RefundReason reason) throws SQLException {
        if (refundDAO.findByOrderId(c, orderId) != null) {
            return null;
        }
        Payment paid = paymentDAO.findPaidByOrderId(c, orderId);
        double amount = paid != null ? paid.getAmount() : codCollectedAmount;
        if (amount <= 0) {
            return null;
        }
        if (paid != null) {
            paymentDAO.updateStatus(c, paid.getId(), PaymentStatus.REFUNDED, paid.getGatewayTxnId());
        }
        Refund r = new Refund();
        r.setId(UUID.randomUUID().toString());
        r.setOrderId(orderId);
        r.setPaymentId(paid != null ? paid.getId() : null);
        r.setBuyerId(buyerId);
        r.setAmount(amount);
        r.setReason(reason);
        r.setStatus(RefundStatus.DONE);
        r.setRefundDate(LocalDateTime.now());
        refundDAO.insert(c, r);
        return r;
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
