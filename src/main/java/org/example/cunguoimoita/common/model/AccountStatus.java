package org.example.cunguoimoita.common.model;

public enum AccountStatus {
    ACTIVE("active"),
    BANNED("banned");

    private final String databaseValue;

    AccountStatus(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String databaseValue() {
        return databaseValue;
    }

    public static AccountStatus fromDatabase(String value) {
        for (AccountStatus status : values()) {
            if (status.databaseValue.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Trạng thái tài khoản không hợp lệ");
    }
}
