package org.example.cunguoimoita.common.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Đọc/ghi tối thiểu bảng orders cho F6 (vận chuyển) và F7 (chấp nhận giá tạo đơn).
 * TẠM THỜI: khi TV3 làm xong OrderDAO/OrderService thì chuyển sang gọi của TV3 và bỏ class này.
 * Mọi hàm nhận Connection để chạy chung transaction với nghiệp vụ gọi nó.
 */
public class OrderRefDAO {

    /** Vài cột của Order mà F6 cần, kèm thành phố gửi (địa chỉ mặc định người bán) và thành phố nhận. */
    public record OrderRef(String id, String code, String buyerId, String sellerId, String status,
                           String fromCity, String toCity) {
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

    /**
     * PriceOffer.accept(): Order — tạo đơn 1 món với giá đã chốt, giao tới địa chỉ mặc định của người mua.
     * paymentMethod tạm để cod, người mua đổi ở bước thanh toán (Order.choosePaymentMethod của TV3).
     * @return id của đơn mới
     */
    public String createFromOffer(Connection c, String offerId, String buyerId, String sellerId,
                                  String productId, double amount) throws SQLException {
        String addressId = findBuyerAddress(c, buyerId);
        if (addressId == null) {
            throw new IllegalStateException("Người mua chưa có địa chỉ giao hàng, cần thêm địa chỉ trước khi chốt đơn");
        }
        String orderId = UUID.randomUUID().toString();
        String code = "ORD-" + orderId.substring(0, 8).toUpperCase();
        String sql = "INSERT INTO orders (id, code, buyer_id, seller_id, shipping_address_id, offer_id, payment_method,"
                + " sub_total, shipping_fee, discount_amount, total_amount, status)"
                + " VALUES (?, ?, ?, ?, ?, ?, 'cod', ?, 0, 0, ?, 'new')";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, orderId);
            ps.setString(2, code);
            ps.setString(3, buyerId);
            ps.setString(4, sellerId);
            ps.setString(5, addressId);
            ps.setString(6, offerId);
            ps.setDouble(7, amount);
            ps.setDouble(8, amount);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO order_items (id, order_id, product_id, quantity, unit_price) VALUES (?, ?, ?, 1, ?)")) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, orderId);
            ps.setString(3, productId);
            ps.setDouble(4, amount);
            ps.executeUpdate();
        }
        return orderId;
    }

    public String findCode(Connection c, String orderId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT code FROM orders WHERE id = ?")) {
            ps.setString(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    /** Order.chooseCarrier(carrierId): ghi phí ship và tính lại totalAmount = subTotal + shippingFee − discountAmount. */
    public void updateShippingFee(Connection c, String orderId, double shippingFee) throws SQLException {
        String sql = "UPDATE orders SET shipping_fee = ?, total_amount = sub_total + ? - discount_amount WHERE id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDouble(1, shippingFee);
            ps.setDouble(2, shippingFee);
            ps.setString(3, orderId);
            ps.executeUpdate();
        }
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

    private String findBuyerAddress(Connection c, String buyerId) throws SQLException {
        String sql = "SELECT id FROM addresses WHERE customer_id = ? ORDER BY is_default DESC, id LIMIT 1";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, buyerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }
}
