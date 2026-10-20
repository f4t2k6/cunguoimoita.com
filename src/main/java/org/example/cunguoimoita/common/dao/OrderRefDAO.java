package org.example.cunguoimoita.common.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/** Truy vấn bảng orders cho vận chuyển và chat. Mọi hàm nhận Connection để chạy chung transaction. */
public class OrderRefDAO {

    /** Vài cột của Order cho vận chuyển, kèm thành phố gửi (địa chỉ mặc định người bán) và thành phố nhận. */
    public record OrderRef(String id, String code, String buyerId, String sellerId, String status,
                           String fromCity, String toCity) {
    }

    /** Đơn vừa tạo: id và mã đơn hiển thị cho người dùng. */
    public record NewOrder(String id, String code) {
    }

    public OrderRef find(Connection c, String orderId) throws SQLException {
        String sql = "SELECT o.id, o.code, o.buyer_id, o.seller_id, o.status, sa.city AS from_city, ta.city AS to_city"
                + " FROM orders o"
                + " JOIN addresses ta ON ta.id = o.shipping_address_id"
                + " LEFT JOIN addresses sa ON sa.customer_id = o.seller_id AND sa.is_default"
                + " WHERE o.id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new OrderRef(rs.getString("id"), rs.getString("code"), rs.getString("buyer_id"),
                        rs.getString("seller_id"), rs.getString("status"),
                        rs.getString("from_city"), rs.getString("to_city"));
            }
        }
    }

    /** Đơn tạo từ các offer của một cuộc trò chuyện, theo id offer (nút "Xem đơn" trong chat). */
    public Map<String, NewOrder> findByConversationOffers(Connection c, String conversationId) throws SQLException {
        String sql = "SELECT o.offer_id, o.id, o.code FROM orders o"
                + " JOIN price_offers po ON po.id = o.offer_id"
                + " WHERE po.conversation_id = ?";
        Map<String, NewOrder> orders = new HashMap<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, conversationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    orders.put(rs.getString("offer_id"), new NewOrder(rs.getString("id"), rs.getString("code")));
                }
            }
        }
        return orders;
    }

    /** Đổi trạng thái đơn, chỉ khi đơn đang ở một trong các trạng thái fromStatuses. @return true nếu có đổi */
    public boolean updateStatus(Connection c, String orderId, String newStatus, String... fromStatuses)
            throws SQLException {
        String sql = "UPDATE orders SET status = ? WHERE id = ? AND status = ANY (?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setString(2, orderId);
            ps.setArray(3, c.createArrayOf("varchar", fromStatuses));
            return ps.executeUpdate() > 0;
        }
    }
}
