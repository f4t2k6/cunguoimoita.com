package org.example.cunguoimoita.dispute.model;

/**
 * Lý do hoàn tiền (Ánh xạ từ Class Diagram Final).
 */
public enum RefundReason {
    CANCELLED("cancelled", "Đơn hàng đã huỷ trước khi gửi"),
    NOT_RECEIVED("not_received", "Không nhận được hàng"),
    WRONG_ITEM("wrong_item", "Giao sai hàng / không đúng mô tả"),
    DISPUTE_WON("dispute_won", "Thắng khiếu nại tranh chấp (Admin phán quyết)");

    private final String dbValue;
    private final String displayName;

    RefundReason(String dbValue, String displayName) {
        this.dbValue = dbValue;
        this.displayName = displayName;
    }

    public String dbValue() {
        return dbValue;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static RefundReason fromDb(String val) {
        if (val == null) return DISPUTE_WON;
        for (RefundReason r : values()) {
            if (r.dbValue.equalsIgnoreCase(val) || r.name().equalsIgnoreCase(val)) {
                return r;
            }
        }
        return DISPUTE_WON;
    }
}
