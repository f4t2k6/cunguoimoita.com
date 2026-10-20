package org.example.cunguoimoita.order.model;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Cart {
    private String id;
    private String ownerId;
    private LocalDate createdDate;

    private List<CartItem> items = new ArrayList<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public LocalDate getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDate createdDate) { this.createdDate = createdDate; }
    public List<CartItem> getItems() { return items; }
    public void setItems(List<CartItem> items) { this.items = items; }

    public CartItem addItem(String productId) {
        if (contains(productId)) {
            throw new IllegalStateException("Sản phẩm đã có trong giỏ hàng");
        }
        CartItem item = new CartItem();
        item.setCartId(id);
        item.setProductId(productId);
        items.add(item);
        return item;
    }

    public void removeItem(String productId) {
        boolean removed = items.removeIf(item -> item.getProductId().equals(productId));
        if (!removed) {
            throw new IllegalStateException("Sản phẩm không có trong giỏ hàng");
        }
    }

    public void clearCart() {
        items.clear();
    }

    public boolean contains (String productId) {
        for (CartItem item : items) {
            if (item.getProductId().equals(productId)) {
                return true;
            }
        }
        return false;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}
