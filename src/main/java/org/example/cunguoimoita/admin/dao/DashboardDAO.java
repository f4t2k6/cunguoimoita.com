package org.example.cunguoimoita.admin.dao;

import org.example.cunguoimoita.admin.model.DashboardStats;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Số liệu trang tổng quan, gom trong 1 câu lệnh (1 lần gọi database). */
public class DashboardDAO {

    public DashboardStats load(Connection c) throws SQLException {
        String sql = "SELECT"
                + " (SELECT COUNT(*) FROM users WHERE user_role = 'customer') AS customers,"
                + " (SELECT COUNT(*) FROM users WHERE status = 'banned') AS banned_users,"
                + " (SELECT COUNT(*) FROM products) AS products,"
                + " (SELECT COUNT(*) FROM products WHERE status = 'available') AS available_products,"
                + " (SELECT COUNT(*) FROM products WHERE approved_by_admin_id IS NULL"
                + "    AND verified_status <> 'rejected' AND status = 'draft') AS pending_products,"
                + " (SELECT COUNT(*) FROM reports WHERE status = 'pending') AS pending_reports,"
                + " (SELECT COUNT(*) FROM orders) AS orders,"
                + " (SELECT COUNT(*) FROM disputes WHERE status IN ('open', 'reviewing')) AS open_disputes,"
                // Tổng giá trị giao dịch: đơn đã giao / hoàn tất
                + " (SELECT COALESCE(SUM(total_amount), 0) FROM orders"
                + "    WHERE status IN ('delivered', 'completed')) AS gmv,"
                // Doanh thu phí của sàn: hoa hồng trên các khoản đã giải ngân cho người bán (F9)
                + " (SELECT COALESCE(SUM(commission_fee), 0) FROM payouts WHERE status = 'done') AS commission";
        try (PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return new DashboardStats(rs.getInt("customers"), rs.getInt("banned_users"), rs.getInt("products"),
                    rs.getInt("available_products"), rs.getInt("pending_products"), rs.getInt("pending_reports"),
                    rs.getInt("orders"), rs.getInt("open_disputes"), rs.getDouble("gmv"), rs.getDouble("commission"));
        }
    }
}
