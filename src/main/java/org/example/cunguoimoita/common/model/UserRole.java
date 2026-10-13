package org.example.cunguoimoita.common.model;

public enum UserRole {
    CUSTOMER("customer"),
    ADMIN("admin");

    private final String databaseValue;

    UserRole(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String databaseValue() {
        return databaseValue;
    }

    public static UserRole fromDatabase(String value) {
        for (UserRole role : values()) {
            if (role.databaseValue.equalsIgnoreCase(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Vai trò tài khoản không hợp lệ");
    }
}
