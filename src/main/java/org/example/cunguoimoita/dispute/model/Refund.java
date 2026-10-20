package org.example.cunguoimoita.dispute.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Refund implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String orderId;
    private String paymentId;
    private String buyerId;
    private double amount;
    private RefundReason reason;
    private RefundStatus status;
    private LocalDateTime refundDate;

    public Refund() {
        this.status = RefundStatus.PENDING;
        this.refundDate = LocalDateTime.now();
    }

    public Refund(String id, String orderId, String paymentId, String buyerId,
                  double amount, RefundReason reason, RefundStatus status, LocalDateTime refundDate) {
        this.id = id;
        this.orderId = orderId;
        this.paymentId = paymentId;
        this.buyerId = buyerId;
        this.amount = amount;
        this.reason = reason;
        this.status = status;
        this.refundDate = refundDate;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(String buyerId) {
        this.buyerId = buyerId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public RefundReason getReason() {
        return reason;
    }

    public void setReason(RefundReason reason) {
        this.reason = reason;
    }

    public RefundStatus getStatus() {
        return status;
    }

    public void setStatus(RefundStatus status) {
        this.status = status;
    }

    public LocalDateTime getRefundDate() {
        return refundDate;
    }

    public void setRefundDate(LocalDateTime refundDate) {
        this.refundDate = refundDate;
    }
}
