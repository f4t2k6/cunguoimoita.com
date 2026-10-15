package org.example.cunguoimoita.dispute.model;

/**
 * Trạng thái hồ sơ tranh chấp / khiếu nại.
 */
public enum DisputeStatus {
    OPEN("open", "Mới gửi (Chờ bên bị khiếu nại phản hồi)"),
    REVIEWING("reviewing", "Đang thẩm định chứng cứ"),
    RESOLVED_REFUND("resolved_refund", "Chấp thuận khiếu nại (Hoàn tiền người mua)"),
    RESOLVED_PAYOUT("resolved_payout", "Bác khiếu nại (Giải ngân cho người bán)"),
    REJECTED("rejected", "Từ chối xử lý khiếu nại");

    private final String dbValue;
    private final String displayName;

    DisputeStatus(String dbValue, String displayName) {
        this.dbValue = dbValue;
        this.displayName = displayName;
    }

    public String dbValue() {
        return dbValue;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static DisputeStatus fromDb(String val) {
        if (val == null) return OPEN;
        for (DisputeStatus s : values()) {
            if (s.dbValue.equalsIgnoreCase(val) || s.name().equalsIgnoreCase(val)) {
                return s;
            }
        }
        return OPEN;
    }
}
