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

    /** Mọi hãng, cả hãng đã tắt (trang admin). */
    public List<ShippingCarrier> findAll() throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM shipping_carriers ORDER BY is_active DESC, base_fee, name";
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

    /** ShippingCarrier.managePartner(): thêm hãng mới (F10). */
    public void insert(ShippingCarrier s) throws SQLException {
        String sql = "INSERT INTO shipping_carriers (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            fill(ps, s);
            ps.executeUpdate();
        }
    }

    /** ShippingCarrier.managePartner(): sửa thông tin, phí cơ bản, bật/tắt (F10). @return true nếu có hãng này */
    public boolean update(ShippingCarrier s) throws SQLException {
        String sql = "UPDATE shipping_carriers SET id = ?, name = ?, logo_url = ?, hotline = ?, base_fee = ?,"
                + " estimated_days = ?, is_active = ? WHERE id = ?";
        try (Connection c = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            fill(ps, s);
            ps.setString(8, s.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean setActive(String id, boolean active) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE shipping_carriers SET is_active = ? WHERE id = ?")) {
            ps.setBoolean(1, active);
            ps.setString(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    private static void fill(PreparedStatement ps, ShippingCarrier s) throws SQLException {
        ps.setString(1, s.getId());
        ps.setString(2, s.getName());
        ps.setString(3, s.getLogoUrl());
        ps.setString(4, s.getHotline());
        ps.setDouble(5, s.getBaseFee());
        ps.setInt(6, s.getEstimatedDays());
        ps.setBoolean(7, s.isActive());
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
