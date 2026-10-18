package org.example.cunguoimoita.order.model;

/** Class OrderItem trong class diagram: một món trong đơn, giá chốt tại thời điểm mua. */
public class OrderItem {

    private String id;
    private String orderId;
    private String productId;
    private int quantity;
    private double unitPrice;   // giá chốt lúc mua, không đổi dù sau này người bán sửa giá

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }
}
