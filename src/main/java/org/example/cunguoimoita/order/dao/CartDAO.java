package org.example.cunguoimoita.order.dao;

import org.example.cunguoimoita.order.model.Cart;
import org.example.cunguoimoita.order.model.CartItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CartDAO {

    // Một dòng hiển thị trong giỏ: món + thông tin sản phẩm + người bán (JOIN với products, users).
    // Dùng cho trang giỏ hàng và cho checkout (cần sellerId, price để tách đơn).
    // coverImage: ảnh đầu tiên của sản phẩm (null nếu bài không có ảnh), để trang giỏ hiện ảnh nhỏ.
    public record CartLine(String productId, String productName, double price,
                           String sellerId, String sellerName, String productStatus, String coverImage) {
        // Còn mua được (chưa có người đặt / chưa bán) → trang giỏ cho tick chọn
        public boolean available() {
            return "available".equals(productStatus);
        }
    }

    public Cart findByOwner(Connection c, String ownerId) throws SQLException {
        String sql = "SELECT id, owner_id, created_date FROM carts WHERE owner_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, ownerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Cart cart = new Cart();
                cart.setId(rs.getString("id"));
                cart.setOwnerId(rs.getString("owner_id"));
                cart.setCreatedDate(rs.getObject("created_date", java.time.LocalDate.class));
                cart.setItems(findItems(c, cart.getId()));
                return cart;
            }
        }
    }

    // Tạo giỏ mới cho khách chưa có giỏ (khách đăng ký sau dữ liệu mẫu).
    public Cart create(Connection c, String ownerId) throws SQLException {
        Cart cart = new Cart();
        cart.setId(UUID.randomUUID().toString());
        cart.setOwnerId(ownerId);
        String sql = "INSERT INTO carts (id, owner_id) VALUES (?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, cart.getId());
            ps.setString(2, ownerId);
            ps.executeUpdate();
        }
        return cart;
    }

    public void insertItem(Connection c, CartItem item) throws SQLException {
        item.setId(UUID.randomUUID().toString());
        String sql = "INSERT INTO cart_items (id, cart_id, product_id, quantity) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, item.getId());
            ps.setString(2, item.getCartId());
            ps.setString(3, item.getProductId());
            ps.setInt(4, item.getQuantity());
            ps.executeUpdate();
        }
    }

    public void deleteItem(Connection c, String cartId, String productId) throws SQLException {
        String sql = "DELETE FROM cart_items WHERE cart_id = ? AND product_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, cartId);
            ps.setString(2, productId);
            ps.executeUpdate();
        }
    }

    // Xoá nhiều món một lúc (sau khi checkout, bỏ các món đã đặt ra khỏi giỏ).
    public void deleteItems(Connection c, String cartId, List<String> productIds) throws SQLException {
        for (String productId : productIds) {
            deleteItem(c, cartId, productId);
        }
    }

    public void clear(Connection c, String cartId) throws SQLException {
        String sql = "DELETE FROM cart_items WHERE cart_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, cartId);
            ps.executeUpdate();
        }
    }

    // Các dòng hiển thị của giỏ, sắp theo người bán để trang giỏ nhóm theo shop.
    public List<CartLine> findLines(Connection c, String cartId) throws SQLException {
        String sql = "SELECT p.id, p.name, p.price, p.seller_id, u.fullname AS seller_name, p.status, p.images"
                + " FROM cart_items ci"
                + " JOIN products p ON p.id = ci.product_id"
                + " JOIN users u ON u.id = p.seller_id"
                + " WHERE ci.cart_id = ?"
                + " ORDER BY p.seller_id, p.name";
        List<CartLine> lines = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, cartId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String images = rs.getString("images");
                    String cover = images == null || images.isBlank() ? null : images.split(",")[0].trim();
                    lines.add(new CartLine(rs.getString("id"), rs.getString("name"), rs.getDouble("price"),
                            rs.getString("seller_id"), rs.getString("seller_name"), rs.getString("status"), cover));
                }
            }
        }
        return lines;
    }

    private List<CartItem> findItems(Connection c, String cartId) throws SQLException {
        String sql = "SELECT id, cart_id, product_id, quantity FROM cart_items WHERE cart_id = ?";
        List<CartItem> items = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, cartId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CartItem item = new CartItem();
                    item.setId(rs.getString("id"));
                    item.setCartId(rs.getString("cart_id"));
                    item.setProductId(rs.getString("product_id"));
                    item.setQuantity(rs.getInt("quantity"));
                    items.add(item);
                }
            }
        }
        return items;
    }
}