package org.example.cunguoimoita.payment.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Thực thể Payment ánh xạ 100% từ Class Diagram:
 * Payment (id, code, orderId, payerId, amount, gateway, gatewayTxnId, status, createdAt, paidAt)
 */
public class Payment implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String code;
    private String orderId;
    private String payerId;
    private double amount;
    private PaymentGateway gateway;
    private String gatewayTxnId;
    private PaymentStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;

    public Payment() {
        this.status = PaymentStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public Payment(String id, String code, String orderId, String payerId, double amount,
                   PaymentGateway gateway, String gatewayTxnId, PaymentStatus status,
                   LocalDateTime createdAt, LocalDateTime paidAt) {
        this.id = id;
        this.code = code;
        this.orderId = orderId;
        this.payerId = payerId;
        this.amount = amount;
        this.gateway = gateway;
        this.gatewayTxnId = gatewayTxnId;
        this.status = status;
        this.createdAt = createdAt;
        this.paidAt = paidAt;
    }

    // Các hàm nghiệp vụ theo Class Diagram
    public void markPaid(String gatewayTxnId) {
        this.status = PaymentStatus.PAID;
        this.gatewayTxnId = gatewayTxnId;
        this.paidAt = LocalDateTime.now();
    }

    public void markFailed() {
        this.status = PaymentStatus.FAILED;
    }

    public void refund() {
        this.status = PaymentStatus.REFUNDED;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getPayerId() {
        return payerId;
    }

    public void setPayerId(String payerId) {
        this.payerId = payerId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public PaymentGateway getGateway() {
        return gateway;
    }

    public void setGateway(PaymentGateway gateway) {
        this.gateway = gateway;
    }

    public String getGatewayTxnId() {
        return gatewayTxnId;
    }

    public void setGatewayTxnId(String gatewayTxnId) {
        this.gatewayTxnId = gatewayTxnId;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }
}
