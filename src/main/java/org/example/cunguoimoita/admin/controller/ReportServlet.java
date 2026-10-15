package org.example.cunguoimoita.admin.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.cunguoimoita.admin.model.ReportReason;
import org.example.cunguoimoita.admin.service.AdminService;
import org.example.cunguoimoita.common.util.CurrentUser;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Phía người dùng của F10: nút "Báo cáo" trên bài đăng / hồ sơ trỏ tới đây.
 * GET  /report?type=product|user&id=...   form báo cáo. Attribute gửi sang JSP: type, id, targetName, reasons, error, sent
 * POST /report   field: type, id, reason (scam | fake_item | prohibited_item | spam | other), description
 * Phải đăng nhập bằng tài khoản khách hàng (bảng reports.reporter_id trỏ customers).
 */
@WebServlet("/report")
public class ReportServlet extends HttpServlet {

    private final AdminService adminService = new AdminService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (CurrentUser.id(req) == null) {
            CurrentUser.redirectToLogin(req, resp);
            return;
        }
        showForm(req, resp, null);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String userId = CurrentUser.id(req);
        if (userId == null) {
            CurrentUser.redirectToLogin(req, resp);
            return;
        }
        if (CurrentUser.isAdmin(req)) {
            showForm(req, resp, "Tài khoản admin xử lý vi phạm ở trang Quản trị, không gửi báo cáo");
            return;
        }
        try {
            adminService.submitReport(userId, req.getParameter("type"), req.getParameter("id"),
                    req.getParameter("reason"), req.getParameter("description"));
            req.setAttribute("sent", true);
            showForm(req, resp, null);
        } catch (IllegalArgumentException | IllegalStateException e) {
            showForm(req, resp, e.getMessage());
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void showForm(HttpServletRequest req, HttpServletResponse resp, String error)
            throws ServletException, IOException {
        String type = req.getParameter("type");
        String id = req.getParameter("id");
        try {
            req.setAttribute("targetName", adminService.reportTargetName(type, id));
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        req.setAttribute("type", type);
        req.setAttribute("id", id);
        req.setAttribute("reasons", ReportReason.values());
        req.setAttribute("error", error);
        req.getRequestDispatcher("/WEB-INF/views/admin/report-form.jsp").forward(req, resp);
    }
}
