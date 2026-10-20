package org.example.cunguoimoita.common.util;

/** Xử lý chuỗi dùng chung cho servlet và service. */
public final class Texts {

    private Texts() {
    }

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    public static String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }
}
