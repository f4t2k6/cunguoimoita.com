package org.example.cunguoimoita.admin.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/**
 * GET /admin/dashboard → trang tổng quan: số người dùng, bài đăng, đơn hàng, doanh thu phí, việc đang chờ.
 * Attribute gửi sang JSP: stats (DashboardStats). Quyền được AdminAuthFilter kiểm tra.
 */
@WebServlet("/admin/dashboard")
public class AdminDashboardServlet extends AdminBaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            req.setAttribute("stats", adminService.stats());
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        render(req, resp, "dashboard.jsp", "dashboard");
    }
}
