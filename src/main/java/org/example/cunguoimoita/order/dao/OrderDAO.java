package org.example.cunguoimoita.order.dao;

import org.example.cunguoimoita.order.model.Order;
import org.example.cunguoimoita.order.model.OrderItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Chạy SQL trên bảng orders và order_items.
public class OrderDAO {

    // Lưu đơn mới kèm toàn bộ món (Order ◆── OrderItem: lưu đơn là lưu luôn món).
    public void insert(Connection c, Order o) throws SQLException {
        String sql = "INSERT INTO orders (id, code, buyer_id, seller_id, shipping_address_id, voucher_id, offer_id,"
                + " payment_method, sub_total, shipping_fee, discount_amount, total_amount, status, order_date)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, o.getId());
            ps.setString(2, o.getCode());
            ps.setString(3, o.getBuyerId());
            ps.setString(4, o.getSellerId());
            ps.setString(5, o.getShippingAddressId());
            ps.setString(6, o.getVoucherId());             // null được: cột cho phép NULL
            ps.setString(7, o.getOfferId());
            ps.setString(8, o.getPaymentMethod().name().toLowerCase());   // COD → "cod"
            ps.setDouble(9, o.getSubTotal());
            ps.setDouble(10, o.getShippingFee());
            ps.setDouble(11, o.getDiscountAmount());
            ps.setDouble(12, o.getTotalAmount());
            ps.setString(13, o.getStatus().name().toLowerCase());          // NEW → "new"
            ps.setObject(14, o.getOrderDate());
            ps.executeUpdate();
        }
        for (OrderItem item : o.getItems()) {
            insertItem(c, item);
        }
    }

    // Tìm 1 đơn theo id (kèm món). Không có → null.
    public Order findById(Connection c, String orderId) throws SQLException {
        return findOne(c, "SELECT * FROM orders WHERE id = ?", orderId);
    }

    // Giống findById nhưng KHOÁ dòng đơn tới hết transaction ("FOR UPDATE"),
    // để 2 thao tác cùng lúc (vd người mua huỷ + người bán gửi hàng) không giẫm lên nhau.
    public Order findByIdForUpdate(Connection c, String orderId) throws SQLException {
        return findOne(c, "SELECT * FROM orders WHERE id = ? FOR UPDATE", orderId);
    }

    // Đơn mua của tôi (mới nhất trước). Danh sách không cần món nên không nạp items.
    public List<Order> findByBuyer(Connection c, String buyerId) throws SQLException {
        return findMany(c, "SELECT * FROM orders WHERE buyer_id = ? ORDER BY order_date DESC", buyerId);
    }

    // Đơn bán của tôi.
    public List<Order> findBySeller(Connection c, String sellerId) throws SQLException {
        return findMany(c, "SELECT * FROM orders WHERE seller_id = ? ORDER BY order_date DESC", sellerId);
    }

    // Lưu lại các cột có thể thay đổi sau khi gọi method của Order (chooseAddress, applyVoucher, cancel...).
    public void update(Connection c, Order o) throws SQLException {
        String sql = "UPDATE orders SET shipping_address_id = ?, voucher_id = ?, payment_method = ?,"
                + " sub_total = ?, shipping_fee = ?, discount_amount = ?, total_amount = ?,"
                + " status = ?, completed_date = ? WHERE id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, o.getShippingAddressId());
            ps.setString(2, o.getVoucherId());
            ps.setString(3, o.getPaymentMethod().name().toLowerCase());
            ps.setDouble(4, o.getSubTotal());
            ps.setDouble(5, o.getShippingFee());
            ps.setDouble(6, o.getDiscountAmount());
            ps.setDouble(7, o.getTotalAmount());
            ps.setString(8, o.getStatus().name().toLowerCase());
            ps.setObject(9, o.getCompletedDate());
            ps.setString(10, o.getId());
            ps.executeUpdate();
        }
    }

    // Sinh id và mã đơn cho đơn mới (vd ORD-1A2B3C4D).
    public static String newId() {
        return UUID.randomUUID().toString();
    }

    public static String codeFor(String orderId) {
        return "ORD-" + orderId.substring(0, 8).toUpperCase();
    }

    // ===================== HÀM PHỤ =====================

    private void insertItem(Connection c, OrderItem item) throws SQLException {
        String sql = "INSERT INTO order_items (id, order_id, product_id, quantity, unit_price) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, item.getId());
            ps.setString(2, item.getOrderId());
            ps.setString(3, item.getProductId());
            ps.setInt(4, item.getQuantity());
            ps.setDouble(5, item.getUnitPrice());
            ps.executeUpdate();
        }
    }

    private List<OrderItem> findItems(Connection c, String orderId) throws SQLException {
        String sql = "SELECT * FROM order_items WHERE order_id = ?";
        List<OrderItem> items = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OrderItem item = new OrderItem();
                    item.setId(rs.getString("id"));
                    item.setOrderId(rs.getString("order_id"));
                    item.setProductId(rs.getString("product_id"));
                    item.setQuantity(rs.getInt("quantity"));
                    item.setUnitPrice(rs.getDouble("unit_price"));
                    items.add(item);
                }
            }
        }
        return items;
    }

    // Chạy câu SELECT trả về 1 đơn (có nạp món).
    private Order findOne(Connection c, String sql, String param) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Order o = map(rs);
                o.setItems(findItems(c, o.getId()));
                return o;
            }
        }
    }

    // Chạy câu SELECT trả về nhiều đơn (không nạp món).
    private List<Order> findMany(Connection c, String sql, String param) throws SQLException {
        List<Order> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    // Chuyển 1 dòng ResultSet → object Order.
    private Order map(ResultSet rs) throws SQLException {
        Order o = new Order();
        o.setId(rs.getString("id"));
        o.setCode(rs.getString("code"));
        o.setBuyerId(rs.getString("buyer_id"));
        o.setSellerId(rs.getString("seller_id"));
        o.setShippingAddressId(rs.getString("shipping_address_id"));
        o.setVoucherId(rs.getString("voucher_id"));
        o.setOfferId(rs.getString("offer_id"));
        o.setPaymentMethod(Order.PaymentMethod.valueOf(rs.getString("payment_method").toUpperCase()));
        o.setSubTotal(rs.getDouble("sub_total"));
        o.setShippingFee(rs.getDouble("shipping_fee"));
        o.setDiscountAmount(rs.getDouble("discount_amount"));
        o.setTotalAmount(rs.getDouble("total_amount"));
        o.setStatus(Order.Status.valueOf(rs.getString("status").toUpperCase()));
        o.setOrderDate(rs.getObject("order_date", LocalDateTime.class));
        o.setCompletedDate(rs.getObject("completed_date", LocalDateTime.class));
        return o;
    }
}