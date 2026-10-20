package org.example.cunguoimoita.admin.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.cunguoimoita.admin.model.ReportReason;
import org.example.cunguoimoita.admin.service.AdminService;
import org.example.cunguoimoita.common.util.CurrentUser;
import org.example.cunguoimoita.common.util.MediaStore;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Người dùng gửi báo cáo vi phạm cho bài đăng hoặc người dùng (tối đa 3 ảnh bằng chứng).
 * Chỉ tài khoản khách hàng được báo cáo vì reports.reporter_id trỏ tới customers.
 */
@WebServlet("/report")
@MultipartConfig(maxFileSize = MediaStore.MAX_FILE_SIZE, maxRequestSize = 4 * MediaStore.MAX_FILE_SIZE)
public class ReportServlet extends HttpServlet {

    private static final int MAX_IMAGES = 3;

    private final AdminService adminService = new AdminService();
    private final MediaStore mediaStore = new MediaStore();

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
            String images = String.join(",", mediaStore.saveAll(req.getParts(), "images", MAX_IMAGES, userId));
            adminService.submitReport(userId, req.getParameter("type"), req.getParameter("id"),
                    req.getParameter("reason"), req.getParameter("description"), images);
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
