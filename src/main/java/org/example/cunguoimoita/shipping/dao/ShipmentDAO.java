package org.example.cunguoimoita.shipping.dao;

import org.example.cunguoimoita.shipping.model.CodStatus;
import org.example.cunguoimoita.shipping.model.Shipment;
import org.example.cunguoimoita.shipping.model.ShipmentStatus;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/** Các hàm nhận Connection từ ShippingService để một thao tác chạy trên 1 kết nối, chung transaction. */
public class ShipmentDAO {

    public void insert(Connection c, Shipment s) throws SQLException {
        String sql = "INSERT INTO shipments (id, order_id, carrier_id, tracking_code, shipping_fee, status, "
                + "created_date, cod_amount, cod_status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, s.getId());
            ps.setString(2, s.getOrderId());
            ps.setString(3, s.getCarrierId());
            ps.setString(4, s.getTrackingCode());
            ps.setDouble(5, s.getShippingFee());
            ps.setString(6, s.getStatus().dbValue());
            ps.setDate(7, Date.valueOf(s.getCreatedDate()));
            ps.setDouble(8, s.getCodAmount());
            ps.setString(9, s.getCodStatus().dbValue());
            ps.executeUpdate();
        }
    }

    /** Lưu lại các trường có thể đổi sau khi tạo: hãng + phí (người mua đổi trước khi gửi), mã vận đơn, trạng thái, ngày giao, COD. */
    public void update(Connection c, Shipment s) throws SQLException {
        String sql = "UPDATE shipments SET carrier_id = ?, shipping_fee = ?, tracking_code = ?, status = ?, delivered_date = ?, "
                + "cod_amount = ?, cod_status = ?, cod_collected_at = ?, cod_remitted_at = ? WHERE id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, s.getCarrierId());
            ps.setDouble(2, s.getShippingFee());
            ps.setString(3, s.getTrackingCode());
            ps.setString(4, s.getStatus().dbValue());
            ps.setDate(5, s.getDeliveredDate() == null ? null : Date.valueOf(s.getDeliveredDate()));
            ps.setDouble(6, s.getCodAmount());
            ps.setString(7, s.getCodStatus().dbValue());
            ps.setTimestamp(8, s.getCodCollectedAt() == null ? null : Timestamp.valueOf(s.getCodCollectedAt()));
            ps.setTimestamp(9, s.getCodRemittedAt() == null ? null : Timestamp.valueOf(s.getCodRemittedAt()));
            ps.setString(10, s.getId());
            ps.executeUpdate();
        }
    }

    /** Giống findByOrderId nhưng khoá dòng vận đơn tới hết transaction. */
    public Shipment findByOrderIdForUpdate(Connection c, String orderId) throws SQLException {
        String sql = "SELECT * FROM shipments WHERE order_id = ? FOR UPDATE";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public Shipment findByOrderId(Connection c, String orderId) throws SQLException {
        String sql = "SELECT * FROM shipments WHERE order_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    private Shipment map(ResultSet rs) throws SQLException {
        Shipment s = new Shipment();
        s.setId(rs.getString("id"));
        s.setOrderId(rs.getString("order_id"));
        s.setCarrierId(rs.getString("carrier_id"));
        s.setTrackingCode(rs.getString("tracking_code"));
        s.setShippingFee(rs.getDouble("shipping_fee"));
        s.setStatus(ShipmentStatus.fromDb(rs.getString("status")));
        s.setCreatedDate(rs.getDate("created_date").toLocalDate());
        Date delivered = rs.getDate("delivered_date");
        s.setDeliveredDate(delivered == null ? null : delivered.toLocalDate());
        s.setCodAmount(rs.getDouble("cod_amount"));
        s.setCodStatus(CodStatus.fromDb(rs.getString("cod_status")));
        Timestamp collected = rs.getTimestamp("cod_collected_at");
        s.setCodCollectedAt(collected == null ? null : collected.toLocalDateTime());
        Timestamp remitted = rs.getTimestamp("cod_remitted_at");
        s.setCodRemittedAt(remitted == null ? null : remitted.toLocalDateTime());
        return s;
    }

    /** Số đơn COD người mua đã từ chối nhận (vận đơn bị hoàn). */
    public int countCodRefusals(Connection c, String buyerId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM shipments s JOIN orders o ON s.order_id = o.id "
                   + "WHERE o.buyer_id = ? AND s.status = 'returned' AND s.cod_amount > 0";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, buyerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }
}
