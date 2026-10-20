package org.example.cunguoimoita.order.model;

// Class CartItem trong class diagram: một món nằm trong giỏ.
// Quan hệ CartItem 0..* → 1 Product: chỉ lưu productId.
public class CartItem {

    private String id;
    private String cartId;
    private String productId;
    private int quantity = 1;    // đồ thanh lý mỗi bài 1 món → luôn là 1

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCartId() { return cartId; }
    public void setCartId(String cartId) { this.cartId = cartId; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}