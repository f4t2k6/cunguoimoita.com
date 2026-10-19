package org.example.cunguoimoita.common.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Đọc/ghi tối thiểu bảng products cho các chức năng khác (F7 chat, trả giá).
 * TẠM THỜI: khi TV2 làm xong ProductDAO/ProductService thì chuyển sang gọi của TV2 và bỏ class này.
 * Mọi hàm nhận Connection để chạy chung transaction với nghiệp vụ gọi nó.
 */
public class ProductRefDAO {

    /** Vài cột của Product mà các module khác cần. status theo enum {draft, available, reserved, sold, exchanged, hidden}. */
    public record ProductRef(String id, String sellerId, String name, String status) {
        public boolean isAvailable() {
            return "available".equals(status);
        }

        /** Còn hiện trên sàn để người khác hỏi: available hoặc reserved. */
        public boolean isOpenForChat() {
            return "available".equals(status) || "reserved".equals(status);
        }
    }

    /** Tên + ảnh bìa của món để hiện trong trang đơn hàng (thay cho mã sản phẩm). */
    public record ProductCard(String id, String name, String coverImage) {
    }

    public ProductCard findCard(Connection c, String productId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT id, name, images FROM products WHERE id = ?")) {
            ps.setString(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return new ProductCard(productId, productId, null);
                }
                String images = rs.getString("images");
                String cover = images == null || images.isBlank() ? null : images.split(",")[0].trim();
                return new ProductCard(rs.getString("id"), rs.getString("name"), cover);
            }
        }
    }

    public ProductRef find(Connection c, String productId) throws SQLException {
        return query(c, "SELECT id, seller_id, name, status FROM products WHERE id = ?", productId);
    }

    /** Khoá dòng sản phẩm tới hết transaction, để 2 người không cùng chốt 1 món. */
    public ProductRef findForUpdate(Connection c, String productId) throws SQLException {
        return query(c, "SELECT id, seller_id, name, status FROM products WHERE id = ? FOR UPDATE", productId);
    }

    /** Product.changeStatus(newStatus) / reserve() / markSold() */
    public void updateStatus(Connection c, String productId, String status) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE products SET status = ? WHERE id = ?")) {
            ps.setString(1, status);
            ps.setString(2, productId);
            ps.executeUpdate();
        }
    }

    private ProductRef query(Connection c, String sql, String productId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new ProductRef(rs.getString("id"), rs.getString("seller_id"),
                        rs.getString("name"), rs.getString("status"));
            }
        }
    }
}
