package org.example.cunguoimoita.payment.model;

public enum PaymentStatus {
    PENDING("pending", "Chờ thanh toán"),
    PAID("paid", "Đã thanh toán (Escrow tạm giữ)"),
    FAILED("failed", "Thất bại / Đã huỷ"),
    REFUNDED("refunded", "Đã hoàn tiền cho người mua");

    private final String dbValue;
    private final String displayName;

    PaymentStatus(String dbValue, String displayName) {
        this.dbValue = dbValue;
        this.displayName = displayName;
    }

    public String dbValue() {
        return dbValue;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static PaymentStatus fromDb(String val) {
        if (val == null) return PENDING;
        for (PaymentStatus s : values()) {
            if (s.dbValue.equalsIgnoreCase(val) || s.name().equalsIgnoreCase(val)) {
                return s;
            }
        }
        return PENDING;
    }
}
