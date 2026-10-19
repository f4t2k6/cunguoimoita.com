package org.example.cunguoimoita.admin.dao;

import org.example.cunguoimoita.admin.model.AdminTodo;
import org.example.cunguoimoita.admin.model.DashboardStats;
import org.example.cunguoimoita.admin.model.OrderStatusCount;
import org.example.cunguoimoita.order.model.Order;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Số liệu trang tổng quan, gom trong 1 câu lệnh (1 lần gọi database). */
public class DashboardDAO {

    // Điều kiện "bài chờ duyệt", dùng chung cho tổng quan và số trên menu
    private static final String PENDING_PRODUCT = "approved_by_admin_id IS NULL"
            + " AND verified_status <> 'rejected' AND status = 'draft'";

    public DashboardStats load(Connection c) throws SQLException {
        String sql = "SELECT"
                + " (SELECT COUNT(*) FROM users WHERE user_role = 'customer') AS customers,"
                + " (SELECT COUNT(*) FROM users WHERE status = 'banned') AS banned_users,"
                + " (SELECT COUNT(*) FROM products) AS products,"
                + " (SELECT COUNT(*) FROM products WHERE status = 'available') AS available_products,"
                + " (SELECT COUNT(*) FROM products WHERE " + PENDING_PRODUCT + ") AS pending_products,"
                + " (SELECT COUNT(*) FROM reports WHERE status = 'pending') AS pending_reports,"
                + " (SELECT COUNT(*) FROM orders) AS orders,"
                + " (SELECT COUNT(*) FROM disputes WHERE status IN ('open', 'reviewing')) AS open_disputes,"
                // Tổng giá trị giao dịch: đơn đã giao / hoàn tất
                + " (SELECT COALESCE(SUM(total_amount), 0) FROM orders"
                + "    WHERE status IN ('delivered', 'completed')) AS gmv,"
                // Doanh thu phí của sàn: hoa hồng trên các khoản đã giải ngân cho người bán (F9)
                + " (SELECT COALESCE(SUM(commission_fee), 0) FROM payouts WHERE status = 'done') AS commission,"
                + " (SELECT COUNT(*) FROM users WHERE user_role = 'customer'"
                + "    AND join_date >= CURRENT_DATE - 6) AS new_customers_7d,"
                + " (SELECT COUNT(*) FROM orders WHERE order_date >= CURRENT_DATE - 6) AS orders_7d";
        try (PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return new DashboardStats(rs.getInt("customers"), rs.getInt("banned_users"), rs.getInt("products"),
                    rs.getInt("available_products"), rs.getInt("pending_products"), rs.getInt("pending_reports"),
                    rs.getInt("orders"), rs.getInt("open_disputes"), rs.getDouble("gmv"), rs.getDouble("commission"),
                    rs.getInt("new_customers_7d"), rs.getInt("orders_7d"));
        }
    }

    /** 3 con số việc cần làm cho menu admin (1 câu lệnh nhẹ, chạy ở mỗi trang admin). */
    public AdminTodo todo(Connection c) throws SQLException {
        String sql = "SELECT"
                + " (SELECT COUNT(*) FROM products WHERE " + PENDING_PRODUCT + ") AS pending_products,"
                + " (SELECT COUNT(*) FROM reports WHERE status = 'pending') AS pending_reports,"
                + " (SELECT COUNT(*) FROM disputes WHERE status IN ('open', 'reviewing')) AS open_disputes";
        try (PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return new AdminTodo(rs.getInt("pending_products"), rs.getInt("pending_reports"), rs.getInt("open_disputes"));
        }
    }

    /** Số đơn theo trạng thái, đúng thứ tự vòng đời đơn (trạng thái không có đơn nào thì bỏ qua). */
    public List<OrderStatusCount> orderStatusCounts(Connection c) throws SQLException {
        int[] counts = new int[Order.Status.values().length];
        try (PreparedStatement ps = c.prepareStatement("SELECT status, COUNT(*) FROM orders GROUP BY status");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                for (Order.Status s : Order.Status.values()) {
                    if (s.name().equalsIgnoreCase(rs.getString(1))) {
                        counts[s.ordinal()] = rs.getInt(2);
                    }
                }
            }
        }
        List<OrderStatusCount> list = new ArrayList<>();
        for (Order.Status s : Order.Status.values()) {
            if (counts[s.ordinal()] > 0) {
                list.add(new OrderStatusCount(s, counts[s.ordinal()]));
            }
        }
        return list;
    }
}
