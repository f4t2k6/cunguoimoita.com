package org.example.cunguoimoita.dispute.model;

/**
 * Trạng thái hoàn tiền cho người mua.
 */
public enum RefundStatus {
    PENDING("pending", "Đang xử lý hoàn tiền"),
    DONE("done", "Đã hoàn tất chuyển tiền về ví"),
    FAILED("failed", "Hoàn tiền thất bại");

    private final String dbValue;
    private final String displayName;

    RefundStatus(String dbValue, String displayName) {
        this.dbValue = dbValue;
        this.displayName = displayName;
    }

    public String dbValue() {
        return dbValue;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static RefundStatus fromDb(String val) {
        if (val == null) return PENDING;
        for (RefundStatus s : values()) {
            if (s.dbValue.equalsIgnoreCase(val) || s.name().equalsIgnoreCase(val)) {
                return s;
            }
        }
        return PENDING;
    }
}
