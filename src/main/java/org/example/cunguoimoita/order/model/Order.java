package org.example.cunguoimoita.order.model;

import org.example.cunguoimoita.shipping.model.Shipment;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Order {
    private String id;
    private String code;
    private String buyerId;
    private String sellerId;
    private String shippingAddressId;
    private String voucherId;
    private String offerId;
    private PaymentMethod paymentMethod;
    private double subTotal;
    private double shippingFee;
    private double discountAmount;
    private double totalAmount;
    private Status status = Status.NEW;
    private LocalDateTime orderDate;
    private LocalDateTime completedDate;
    public enum Status {
        NEW, AWAITING_PAYMENT, PAID, SHIPPING, DELIVERED, COMPLETED, CANCELLED, REFUNDED;

        public boolean canMoveTo(Status next) {
            return switch (this) {
                case NEW -> next == AWAITING_PAYMENT || next == PAID
                        || next == SHIPPING || next == CANCELLED;
                case AWAITING_PAYMENT -> next == PAID || next == CANCELLED;
                case PAID -> next == SHIPPING || next == CANCELLED || next == REFUNDED;
                case SHIPPING -> next == DELIVERED || next == REFUNDED;
                case DELIVERED -> next == COMPLETED || next == REFUNDED;
                default -> false;
            };
        }
        public String label() {
            return switch (this) {
                case NEW -> "Mới tạo";
                case AWAITING_PAYMENT -> "Chờ thanh toán";
                case PAID -> "Đã thanh toán";
                case SHIPPING -> "Đang giao";
                case DELIVERED -> "Đã giao";
                case COMPLETED -> "Hoàn tất";
                case CANCELLED -> "Đã huỷ";
                default -> "Đã hoàn tiền";
            };
        }
    }
    public enum PaymentMethod {ONLINE, COD}

    private List<OrderItem> items = new ArrayList<>();
    // subTotal = Σ (số lượng × giá chốt); totalAmount = subTotal + shippingFee − discountAmount
    public double calculateTotal() {
        if (!items.isEmpty()) {
            double sum = 0;
            for (OrderItem item : items) {
                sum += item.getQuantity() * item.getUnitPrice();
            }
            subTotal = sum;
        }
        totalAmount = Math.max(0, subTotal + shippingFee - discountAmount);  // không để tổng bị âm
        return totalAmount;
    }
    // Chỉ đổi được khi đơn chưa thanh toán / chưa giao
    public void chooseAddress(String addressId) {
        requireEditable();
        if (addressId == null || addressId.isBlank()) {
            throw new IllegalStateException("Vui lòng chọn địa chỉ giao hàng");
        }
        this.shippingAddressId = addressId;
    }
    // Phí ship do ShippingCarrier.calculateFee() tính, nên truyền thêm fee vào.
    // Tạo Shipment (vận đơn) và ghi phí ship vào đơn, rồi tính lại tổng.
    public Shipment chooseCarrier (String carrierId, double fee) {
        requireEditable();
        Shipment shipment = new Shipment();
        shipment.setOrderId(id);
        shipment.setCarrierId(carrierId);
        shipment.setShippingFee(fee);
        shipment.setCreatedDate(LocalDate.now());
        this.shippingFee= fee;
        calculateTotal();
        return shipment;
    }
    // online → chuyển sang "chờ thanh toán"; COD → giữ "new", chờ người bán gửi hàng
    public void choosePaymentMethod (PaymentMethod method) {
        requireEditable();
        if(method ==null) {
            throw new IllegalStateException("Vui lòng chọn phương thức thanh toán");

        }
        this.paymentMethod = method;
        this.status = (method == PaymentMethod.ONLINE) ? Status.AWAITING_PAYMENT : Status.NEW;
    }

    public void applyVoucher(Voucher voucher) {
        requireEditable();
        if (voucherId != null) {
            throw new IllegalStateException("Đơn này đã được áp dụng voucher");
        }
        if (!voucher.checkValidity()) {
            throw new IllegalStateException("Voucher đã được sử dụng hoặc đã hết hạn");

        }
        if (!voucher.getCustomerId().equals(buyerId)) {
            throw new IllegalStateException("Voucher này không thuộc quyền sở hữu của bạn");

        }
        if (subTotal < voucher.getMinOrderValue()) {
            throw new IllegalStateException("Đơn chưa đủ giá trị tối thiểu để dfung voucher");

        }
        voucher.redeem(id);
        this.voucherId= voucher.getId();
        this.discountAmount= Math.min(voucher.getDiscountValue(), subTotal); // giảm không quá tiền hàng
        calculateTotal();
    }
    /** Thanh toán online thành công (Payment.markPaid). */
    public void markPaid() {
        moveTo(Status.PAID);
    }

    // Người bán đã gửi hàng
    public void confirmShipped() {
        moveTo(Status.SHIPPING);
    }
    // Người mua bấm "Đã nhận hàng" → đơn hoàn tất
    public void confirmReceived() {
        if (status == Status.SHIPPING) {
            moveTo(Status.DELIVERED);     // shipper chưa báo giao nhưng người mua đã nhận
        }
        complete();
    }

    public void complete() {
        moveTo(Status.COMPLETED);
        this.completedDate = LocalDateTime.now();
    }

    public void cancel(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập lý do huỷ đơn");
        }
        moveTo(Status.CANCELLED);
    }

    // Đổi trạng thái, nhưng phải qua luật canMoveTo() → không ai đổi bậy được (đóng gói)
    private void moveTo(Status next) {
        if (!status.canMoveTo(next)) {
            throw new IllegalStateException("Không thể chuyển đơn từ " + status + " sang " + next);
        }
        this.status = next;
    }

    // Chỉ được sửa địa chỉ / vận chuyển / thanh toán / voucher khi đơn chưa thanh toán
    private void requireEditable() {
        if (status != Status.NEW && status != Status.AWAITING_PAYMENT) {
            throw new IllegalStateException("Đơn đã thanh toán hoặc đang xử lý, không sửa được nữa");
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getBuyerId() { return buyerId; }
    public void setBuyerId(String buyerId) { this.buyerId = buyerId; }
    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    public String getShippingAddressId() { return shippingAddressId; }
    public void setShippingAddressId(String shippingAddressId) { this.shippingAddressId = shippingAddressId; }
    public String getVoucherId() { return voucherId; }
    public void setVoucherId(String voucherId) { this.voucherId = voucherId; }
    public String getOfferId() { return offerId; }
    public void setOfferId(String offerId) { this.offerId = offerId; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public double getSubTotal() { return subTotal; }
    public void setSubTotal(double subTotal) { this.subTotal = subTotal; }
    public double getShippingFee() { return shippingFee; }
    public void setShippingFee(double shippingFee) { this.shippingFee = shippingFee; }
    public double getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(double discountAmount) { this.discountAmount = discountAmount; }
    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public LocalDateTime getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }
    public LocalDateTime getCompletedDate() { return completedDate; }
    public void setCompletedDate(LocalDateTime completedDate) { this.completedDate = completedDate; }
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }

}
