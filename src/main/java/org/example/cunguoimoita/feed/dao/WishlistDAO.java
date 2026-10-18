package org.example.cunguoimoita.feed.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Bucket List (class Wishlist): mỗi khách hàng có 0..1 wishlist,
 * danh sách bài đã lưu nằm ở bảng phụ wishlist_products.
 */
public class WishlistDAO {

    /** id wishlist của khách hàng; chưa có thì tạo mới. */
    public String findOrCreate(Connection c, String customerId) throws SQLException {
        String id = find(c, customerId);
        if (id != null) {
            return id;
        }
        String sql = "INSERT INTO wishlists (id, customer_id) VALUES (?, ?) ON CONFLICT (customer_id) DO NOTHING";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, customerId);
            ps.executeUpdate();
        }
        return find(c, customerId);   // đọc lại phòng trường hợp hai yêu cầu cùng tạo một lúc
    }

    public String find(Connection c, String customerId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT id FROM wishlists WHERE customer_id = ?")) {
            ps.setString(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    /** Wishlist.add(product); lưu trùng thì bỏ qua. */
    public void add(Connection c, String wishlistId, String productId) throws SQLException {
        String sql = "INSERT INTO wishlist_products (wishlist_id, product_id) VALUES (?, ?) ON CONFLICT DO NOTHING";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, wishlistId);
            ps.setString(2, productId);
            ps.executeUpdate();
        }
    }

    /** Wishlist.remove(product) */
    public void remove(Connection c, String wishlistId, String productId) throws SQLException {
        String sql = "DELETE FROM wishlist_products WHERE wishlist_id = ? AND product_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, wishlistId);
            ps.setString(2, productId);
            ps.executeUpdate();
        }
    }

    /** id các bài khách đã lưu (để tô nút "Đã lưu" trên bảng tin). */
    public Set<String> savedProductIds(Connection c, String customerId) throws SQLException {
        String sql = "SELECT wp.product_id FROM wishlist_products wp"
                + " JOIN wishlists w ON w.id = wp.wishlist_id WHERE w.customer_id = ?";
        Set<String> ids = new HashSet<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getString(1));
                }
            }
        }
        return ids;
    }
}
