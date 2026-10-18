package org.example.cunguoimoita.product.model;

/** Loại bài đăng: chỉ bán, chỉ trao đổi, hoặc vừa bán vừa nhận đổi. */
public enum ListingType {
    SALE("sale", "Bán"),
    SWAP("swap", "Trao đổi"),
    BOTH("both", "Bán hoặc đổi");

    private final String code;
    private final String label;

    ListingType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() { return code; }
    public String getLabel() { return label; }

    /** Bài có bán lấy tiền không (để hiện giá và nút Thêm vào giỏ). */
    public boolean isForSale() { return this != SWAP; }

    /** Bài có nhận trao đổi không (để hiện giá trị ước tính, món muốn đổi và gợi ý trao đổi). */
    public boolean isForSwap() { return this != SALE; }

    public static ListingType fromCode(String code) {
        for (ListingType t : values()) {
            if (t.code.equals(code)) {
                return t;
            }
        }
        return null;
    }
}
