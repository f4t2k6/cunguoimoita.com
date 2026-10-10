package org.example.cunguoimoita.shipping.dao;

import org.example.cunguoimoita.common.db.ConnectionPool;
import org.example.cunguoimoita.shipping.model.CodStatus;
import org.example.cunguoimoita.shipping.model.Shipment;
import org.example.cunguoimoita.shipping.model.ShipmentStatus;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class ShipmentDAO {

    public void insert(Shipment s) throws SQLException {
        String sql = "INSERT INTO shipments (id, order_id, carrier_id, tracking_code, shipping_fee, status, "
                + "created_date, cod_amount, cod_status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
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

    /** Lưu lại các trường có thể đổi sau khi tạo: mã vận đơn, trạng thái, ngày giao, COD. */
    public void update(Shipment s) throws SQLException {
        String sql = "UPDATE shipments SET tracking_code = ?, status = ?, delivered_date = ?, "
                + "cod_status = ?, cod_collected_at = ?, cod_remitted_at = ? WHERE id = ?";
        try (Connection c = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, s.getTrackingCode());
            ps.setString(2, s.getStatus().dbValue());
            ps.setDate(3, s.getDeliveredDate() == null ? null : Date.valueOf(s.getDeliveredDate()));
            ps.setString(4, s.getCodStatus().dbValue());
            ps.setTimestamp(5, s.getCodCollectedAt() == null ? null : Timestamp.valueOf(s.getCodCollectedAt()));
            ps.setTimestamp(6, s.getCodRemittedAt() == null ? null : Timestamp.valueOf(s.getCodRemittedAt()));
            ps.setString(7, s.getId());
            ps.executeUpdate();
        }
    }

    public Shipment findByOrderId(String orderId) throws SQLException {
        String sql = "SELECT * FROM shipments WHERE order_id = ?";
        try (Connection c = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
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
}
