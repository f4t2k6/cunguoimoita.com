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
        switch (this) {
            case CREATED:    return next == PICKED_UP || next == RETURNED;
            case PICKED_UP:  return next == IN_TRANSIT || next == RETURNED;
            case IN_TRANSIT: return next == DELIVERED || next == RETURNED;
            default:         return false;
        }
    }

    public String label() {
        switch (this) {
            case CREATED:    return "Chờ người bán xác nhận";
            case PICKED_UP:  return "Chuẩn bị vận chuyển";
            case IN_TRANSIT: return "Đang vận chuyển";
            case DELIVERED:  return "Đã giao thành công";
            default:         return "Hoàn hàng";
        }
    }
}
