package org.example.cunguoimoita.dispute.model;

public enum DisputeReason {
    NOT_RECEIVED("not_received", "Chưa nhận được hàng"),
    WRONG_ITEM("wrong_item", "Giao sai sản phẩm / khác mô tả"),
    DAMAGED("damaged", "Hàng bị bể vỡ / hư hỏng chức năng"),
    FAKE("fake", "Phát hiện hàng giả / hàng nhái"),
    OTHER("other", "Lý do tranh chấp khác");

    private final String dbValue;
    private final String displayName;

    DisputeReason(String dbValue, String displayName) {
        this.dbValue = dbValue;
        this.displayName = displayName;
    }

    public String dbValue() {
        return dbValue;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static DisputeReason fromDb(String val) {
        if (val == null) return OTHER;
        for (DisputeReason r : values()) {
            if (r.dbValue.equalsIgnoreCase(val) || r.name().equalsIgnoreCase(val)) {
                return r;
            }
        }
        return OTHER;
    }
}
