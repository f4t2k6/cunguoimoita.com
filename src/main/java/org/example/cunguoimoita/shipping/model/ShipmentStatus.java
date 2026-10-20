package org.example.cunguoimoita.shipping.model;

/** Shipment.status: enum {created, picked_up, in_transit, delivered, returned} */
public enum ShipmentStatus {
    CREATED, PICKED_UP, IN_TRANSIT, DELIVERED, RETURNED;

    /** Giá trị lưu trong DB (chữ thường như class diagram). */
    public String dbValue() {
        return name().toLowerCase();
    }

    public static ShipmentStatus fromDb(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Thiếu trạng thái vận đơn");
        }
        return valueOf(value.toUpperCase());   // chuỗi lạ: valueOf ném IllegalArgumentException
    }

    /** Chuỗi hợp lệ: created → picked_up → in_transit → delivered, hoặc chuyển returned khi chưa giao xong. */
    public boolean canMoveTo(ShipmentStatus next) {
        return switch (this) {
            case CREATED -> next == PICKED_UP || next == RETURNED;
            case PICKED_UP -> next == IN_TRANSIT || next == RETURNED;
            case IN_TRANSIT -> next == DELIVERED || next == RETURNED;
            default -> false;
        };
    }

    public String label() {
        return switch (this) {
            case CREATED -> "Chờ người bán xác nhận";
            case PICKED_UP -> "Chuẩn bị vận chuyển";
            case IN_TRANSIT -> "Đang vận chuyển";
            case DELIVERED -> "Đã giao thành công";
            default -> "Hoàn hàng";
        };
    }
}
