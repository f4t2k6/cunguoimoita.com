package org.example.cunguoimoita.payment.model;

/**
 * Các cổng thanh toán hỗ trợ trong hệ thống (Ánh xạ từ Class Diagram Final).
 */
public enum PaymentGateway {
    VNPAY("vnpay", "VNPay QR / Thẻ ATM"),
    MOMO("momo", "Ví điện tử MoMo"),
    BANK_TRANSFER("bank_transfer", "Chuyển khoản ngân hàng VietQR");

    private final String dbValue;
    private final String displayName;

    PaymentGateway(String dbValue, String displayName) {
        this.dbValue = dbValue;
        this.displayName = displayName;
    }

    public String dbValue() {
        return dbValue;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static PaymentGateway fromDb(String val) {
        if (val == null) return VNPAY;
        for (PaymentGateway g : values()) {
            if (g.dbValue.equalsIgnoreCase(val) || g.name().equalsIgnoreCase(val)) {
                return g;
            }
        }
        return VNPAY;
    }
}
