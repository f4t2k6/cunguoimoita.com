package org.example.cunguoimoita.product.model;

/**
 * Vòng đời của một bài đăng (Product.status):
 *   draft      chưa hiện trên sàn: đang chờ admin duyệt, bị từ chối, hoặc người bán tự tạm ẩn
 *   available  đã duyệt, đang bán
 *   reserved   có người đã đặt (F4 gọi reserve()), chờ giao dịch xong
 *   sold / exchanged  đã bán / đã đổi xong
 *   hidden     admin ẩn vì vi phạm, người bán không tự mở lại được
 */
public enum ProductStatus {
    DRAFT("draft", "Chưa hiện"),
    AVAILABLE("available", "Đang bán"),
    RESERVED("reserved", "Đã có người đặt"),
    SOLD("sold", "Đã bán"),
    EXCHANGED("exchanged", "Đã đổi"),
    HIDDEN("hidden", "Bị ẩn bởi admin");

    private final String code;
    private final String label;

    ProductStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() { return code; }
    public String getLabel() { return label; }

    public static ProductStatus fromCode(String code) {
        for (ProductStatus s : values()) {
            if (s.code.equals(code)) {
                return s;
            }
        }
        return null;
    }
}
