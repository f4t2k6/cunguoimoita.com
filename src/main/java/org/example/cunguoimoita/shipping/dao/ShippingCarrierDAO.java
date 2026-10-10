package org.example.cunguoimoita.shipping.dao;

import org.example.cunguoimoita.common.db.ConnectionPool;
import org.example.cunguoimoita.shipping.model.ShippingCarrier;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ShippingCarrierDAO {

    private static final String COLUMNS = "id, name, logo_url, hotline, base_fee, estimated_days, is_active";

    public List<ShippingCarrier> findActive() throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM shipping_carriers WHERE is_active = TRUE ORDER BY base_fee";
        List<ShippingCarrier> list = new ArrayList<>();
        try (Connection c = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    public ShippingCarrier findById(String id) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM shipping_carriers WHERE id = ?";
        try (Connection c = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    private ShippingCarrier map(ResultSet rs) throws SQLException {
        return new ShippingCarrier(
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("logo_url"),
                rs.getString("hotline"),
                rs.getDouble("base_fee"),
                rs.getInt("estimated_days"),
                rs.getBoolean("is_active"));
    }
}
