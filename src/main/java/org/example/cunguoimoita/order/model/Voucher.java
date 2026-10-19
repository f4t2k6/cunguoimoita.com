package org.example.cunguoimoita.order.model;

import java.time.LocalDate;

/** Class Voucher trong class diagram. Chỉ chứa luật nghiệp vụ, việc lưu DB do VoucherDAO làm. */
public class Voucher {
    public enum VoucherStatus {ISSUED, USED, EXPIRED}
    private String id;
    private String code;
    private String customerId;          // khách sở hữu voucher
    private double discountValue;       // số tiền được giảm
    private double minOrderValue;       // đơn phải từ mức này trở lên mới dùng được
    private VoucherStatus status = VoucherStatus.ISSUED;
    private LocalDate issuedDate;       // ngày phát hành
    private LocalDate expiryDate;       // ngày hết hạn;  null = không hết hạn

    /** + checkValidity(): boolean — còn dùng được không: chưa dùng và chưa hết hạn. */
    public boolean checkValidity() {
        if (status != VoucherStatus.ISSUED) {
            return false;
        }
        if (expiryDate != null && LocalDate.now().isAfter(expiryDate)) {
            status = VoucherStatus.EXPIRED;
            return false;
        }
        return true;
    }
    /** + redeem(String orderId): void - đánh dấu voucher đã được dùng cho một đơn */
    public void redeem (String orderId) {
        if (!checkValidity()) {
            throw new IllegalStateException("Voucher không còn hiệu lực");
        }
        status = VoucherStatus.USED;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public double getDiscountValue() { return discountValue; }
    public void setDiscountValue(double discountValue) { this.discountValue = discountValue; }
    public double getMinOrderValue() { return minOrderValue; }
    public void setMinOrderValue(double minOrderValue) { this.minOrderValue = minOrderValue; }
    public VoucherStatus getStatus() { return status; }
    public void setStatus(VoucherStatus status) { this.status = status; }
    public LocalDate getIssuedDate() { return issuedDate; }
    public void setIssuedDate(LocalDate issuedDate) { this.issuedDate = issuedDate; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
}
