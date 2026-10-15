package org.example.cunguoimoita.admin.model;

/** Lý do báo cáo, khớp CHECK của reports.reason. */
public enum ReportReason {
    SCAM("Lừa đảo"),
    FAKE_ITEM("Hàng giả"),
    PROHIBITED_ITEM("Hàng cấm"),
    SPAM("Spam"),
    OTHER("Khác");

    private final String label;

    ReportReason(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }

    public String dbValue() { return name().toLowerCase(); }

    public static ReportReason fromDb(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Chưa chọn lý do báo cáo");
        }
        return valueOf(value.toUpperCase());
    }
}
