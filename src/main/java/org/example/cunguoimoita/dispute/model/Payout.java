package org.example.cunguoimoita.dispute.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Thực thể Payout ánh xạ từ Class Diagram Final:
 * Payout (id, orderId, sellerId, grossAmount, commissionFee, netAmount, method, status, payoutDate)
 */
public class Payout implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String orderId;
    private String sellerId;
    private double grossAmount;
    private double commissionFee;
    private double netAmount;
    private PayoutMethod method;
    private PayoutStatus status;
    private LocalDateTime payoutDate;

    public Payout() {
        this.method = PayoutMethod.BANK_TRANSFER;
        this.status = PayoutStatus.SCHEDULED;
    }

    public Payout(String id, String orderId, String sellerId, double grossAmount,
                  double commissionFee, double netAmount, PayoutMethod method,
                  PayoutStatus status, LocalDateTime payoutDate) {
        this.id = id;
        this.orderId = orderId;
        this.sellerId = sellerId;
        this.grossAmount = grossAmount;
        this.commissionFee = commissionFee;
        this.netAmount = netAmount;
        this.method = method;
        this.status = status;
        this.payoutDate = payoutDate;
    }

    /**
     * Phương thức theo Class Diagram:
     * Tính toán số tiền thực nhận sau khi trừ hoa hồng sàn (có chiết khấu theo MemberTier)
     */
    public static double calculateNetAmount(double grossAmount, double commissionRate) {
        double fee = Math.round(grossAmount * commissionRate);
        return Math.max(0, grossAmount - fee);
    }

    public void processPayout() {
        this.status = PayoutStatus.DONE;
        this.payoutDate = LocalDateTime.now();
    }

    // Getters and Setters
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

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public double getGrossAmount() {
        return grossAmount;
    }

    public void setGrossAmount(double grossAmount) {
        this.grossAmount = grossAmount;
    }

    public double getCommissionFee() {
        return commissionFee;
    }

    public void setCommissionFee(double commissionFee) {
        this.commissionFee = commissionFee;
    }

    public double getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(double netAmount) {
        this.netAmount = netAmount;
    }

    public PayoutMethod getMethod() {
        return method;
    }

    public void setMethod(PayoutMethod method) {
        this.method = method;
    }

    public PayoutStatus getStatus() {
        return status;
    }

    public void setStatus(PayoutStatus status) {
        this.status = status;
    }

    public LocalDateTime getPayoutDate() {
        return payoutDate;
    }

    public void setPayoutDate(LocalDateTime payoutDate) {
        this.payoutDate = payoutDate;
    }
}
