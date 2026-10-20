package org.example.cunguoimoita.dispute.model;

public enum PayoutMethod {
    BANK_TRANSFER("bank_transfer", "Chuyển khoản Ngân hàng"),
    EWALLET("ewallet", "Ví điện tử");

    private final String dbValue;
    private final String displayName;

    PayoutMethod(String dbValue, String displayName) {
        this.dbValue = dbValue;
        this.displayName = displayName;
    }

    public String dbValue() {
        return dbValue;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static PayoutMethod fromDb(String val) {
        if (val == null) return BANK_TRANSFER;
        for (PayoutMethod m : values()) {
            if (m.dbValue.equalsIgnoreCase(val) || m.name().equalsIgnoreCase(val)) {
                return m;
            }
        }
        return BANK_TRANSFER;
    }
}
