package org.example.cunguoimoita.order.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Thẻ đơn ở trang Đơn mua / Đơn bán, chỉ để hiển thị. */
public class OrderCard {

    /** Một món trong thẻ: tên, ảnh bìa, số lượng, giá đã chốt. */
    public static class Line {
        private final String productId;
        private final String name;
        private final String image;
        private final int quantity;
        private final double unitPrice;

        public Line(String productId, String name, String image, int quantity, double unitPrice) {
            this.productId = productId;
            this.name = name;
            this.image = image;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
        }

        public String getProductId() { return productId; }
        public String getName() { return name; }
        public String getImage() { return image; }
        public int getQuantity() { return quantity; }
        public double getUnitPrice() { return unitPrice; }
    }

    private final Order order;
    private final boolean seller;          // true: đang xem ở Đơn bán
    private final String partnerName;      // người bán (ở Đơn mua) hoặc người mua (ở Đơn bán)
    private final List<Line> lines = new ArrayList<>();
    private String shipmentStatus;         // created / picked_up / in_transit / delivered / returned, null nếu chưa có vận đơn
    private LocalDate deliveredDate;
    private boolean cod;                   // đã xác nhận trả khi nhận hàng

    public OrderCard(Order order, boolean seller, String partnerName) {
        this.order = order;
        this.seller = seller;
        this.partnerName = partnerName;
    }

    public void addLine(Line line) { lines.add(line); }

    public void setShipment(String status, LocalDate deliveredDate, boolean cod) {
        this.shipmentStatus = status;
        this.deliveredDate = deliveredDate;
        this.cod = cod;
    }

    public Order getOrder() { return order; }
    public String getPartnerName() { return partnerName; }
    public List<Line> getLines() { return lines; }

    public int getItemCount() {
        return lines.stream().mapToInt(Line::getQuantity).sum();
    }

    /** Đã chốt giá nhưng người mua chưa trả tiền (chưa chọn COD, chưa thanh toán online). */
    public boolean isAwaitingPayment() {
        Order.Status s = order.getStatus();
        return s == Order.Status.AWAITING_PAYMENT || (s == Order.Status.NEW && !cod);
    }

    /** Màu khung thông báo: "ok" xanh nhạt, "off" xám (huỷ, hoàn tiền, hoàn hàng). */
    public String getNoticeTone() {
        Order.Status s = order.getStatus();
        return s == Order.Status.CANCELLED || s == Order.Status.REFUNDED || "returned".equals(shipmentStatus) ? "off" : "ok";
    }

    /** Dòng trạng thái trong khung xanh nhạt. */
    public String getNotice() {
        String closed = switch (order.getStatus()) {
            case CANCELLED -> "Đơn đã huỷ";
            case REFUNDED -> "Đã hoàn tiền cho người mua";
            case COMPLETED -> "Đơn đã hoàn tất";
            default -> null;
        };
        if (closed != null) {
            return closed;
        }
        if (isAwaitingPayment()) {
            return seller ? "Đã chốt giá, chờ người mua thanh toán" : "Đã chốt giá, chờ bạn thanh toán để người bán giao hàng";
        }
        if ("returned".equals(shipmentStatus)) return "Đơn vị vận chuyển đã hoàn hàng về người bán";
        if (order.getStatus() == Order.Status.DELIVERED || "delivered".equals(shipmentStatus)) {
            return deliveredDate == null ? "Giao hàng thành công"
                    : "Giao hàng thành công vào " + deliveredDate.getDayOfMonth() + " Thg " + deliveredDate.getMonthValue();
        }
        if ("in_transit".equals(shipmentStatus)) {
            return seller ? "Đơn hàng đang được giao tới người mua" : "Đơn hàng đang trên đường giao đến bạn";
        }
        if ("picked_up".equals(shipmentStatus)) {
            return seller ? "Đã xác nhận giao hàng, chờ đơn vị vận chuyển lấy hàng" : "Người bán đang chuẩn bị vận chuyển";
        }
        return seller ? "Bạn cần xác nhận giao hàng cho đơn vị vận chuyển" : "Chờ người bán xác nhận giao hàng";
    }
}
