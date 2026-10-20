package org.example.cunguoimoita.dispute.model;

public enum PayoutStatus {
    SCHEDULED("scheduled", "Đã lên lịch (Đang giữ chờ đối soát)"),
    DONE("done", "Đã giải ngân thành công"),
    FAILED("failed", "Giải ngân thất bại (Sai số tài khoản)");

    private final String dbValue;
    private final String displayName;

    PayoutStatus(String dbValue, String displayName) {
        this.dbValue = dbValue;
        this.displayName = displayName;
    }

    public String dbValue() {
        return dbValue;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static PayoutStatus fromDb(String val) {
        if (val == null) return SCHEDULED;
        for (PayoutStatus s : values()) {
            if (s.dbValue.equalsIgnoreCase(val) || s.name().equalsIgnoreCase(val)) {
                return s;
            }
        }
        return SCHEDULED;
    }
}
