package org.example.cunguoimoita.admin.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.cunguoimoita.admin.service.AdminService;
import org.example.cunguoimoita.common.util.CurrentUser;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

/**
 * Phần chung của các trang /admin/* (quyền admin đã được AdminAuthFilter kiểm tra trước khi vào đây).
 * Mẫu POST → xử lý → redirect về GET (tránh gửi lại form khi F5); thông báo kết quả cất trong session
 * ("flash") rồi hiện một lần ở trang kế tiếp.
 */
public abstract class AdminBaseServlet extends HttpServlet {

    protected final AdminService adminService = new AdminService();

    @FunctionalInterface
    protected interface Action {
        String run() throws SQLException;   // trả về câu báo thành công
    }

    /** Chạy một thao tác POST rồi quay về trang redirectPath (không kèm context path). */
    protected void handle(HttpServletRequest req, HttpServletResponse resp, String redirectPath, Action action)
            throws ServletException, IOException {
        HttpSession session = req.getSession();
        // Chỉ giữ thông báo của thao tác mới nhất
        session.removeAttribute("adminFlash");
        session.removeAttribute("adminFlashError");
        try {
            session.setAttribute("adminFlash", action.run());
        } catch (IllegalArgumentException | IllegalStateException e) {
            session.setAttribute("adminFlashError", e.getMessage());
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        resp.sendRedirect(req.getContextPath() + redirectPath);
    }

    /** Đưa flash từ session sang request cho JSP rồi xoá, rồi forward tới view. */
    protected void render(HttpServletRequest req, HttpServletResponse resp, String view, String activeTab)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session != null) {
            req.setAttribute("flash", session.getAttribute("adminFlash"));
            req.setAttribute("flashError", session.getAttribute("adminFlashError"));
            session.removeAttribute("adminFlash");
            session.removeAttribute("adminFlashError");
        }
        req.setAttribute("adminTab", activeTab);
        // Số việc đang chờ hiện trên menu (Duyệt bài 3, Báo cáo 1...) để admin biết chỗ cần vào
        try {
            req.setAttribute("adminTodo", adminService.todo());
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        req.getRequestDispatcher("/WEB-INF/views/admin/" + view).forward(req, resp);
    }

    protected static String adminId(HttpServletRequest req) {
        return CurrentUser.id(req);
    }

    /** Giữ lại tham số lọc khi quay về trang danh sách. */
    protected static String query(HttpServletRequest req, String... names) {
        StringBuilder q = new StringBuilder();
        for (String n : names) {
            String v = req.getParameter(n);
            if (v != null && !v.isBlank()) {
                q.append(q.length() == 0 ? '?' : '&').append(n).append('=')
                 .append(URLEncoder.encode(v, StandardCharsets.UTF_8));
            }
        }
        return q.toString();
    }
}
