package org.example.cunguoimoita.product.model;

/**
 * Tình trạng món đồ (Product.condition trong class diagram: mới, cũ, hỏng nhẹ).
 * Database lưu mã không dấu (new | used | damaged), giao diện hiện nhãn tiếng Việt.
 */
public enum ProductCondition {
    NEW("new", "Mới"),
    USED("used", "Đã qua sử dụng"),
    DAMAGED("damaged", "Hỏng nhẹ");

    private final String code;
    private final String label;

    ProductCondition(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() { return code; }
    public String getLabel() { return label; }

    /** Đổi mã trong database (hoặc trong form) thành enum, trả null nếu mã lạ. */
    public static ProductCondition fromCode(String code) {
        for (ProductCondition c : values()) {
            if (c.code.equals(code)) {
                return c;
            }
        }
        return null;
    }
}
