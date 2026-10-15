package org.example.cunguoimoita.admin.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/**
 * GET  /admin/users?q=&status=active|banned   tìm người dùng
 *      Attribute gửi sang JSP: users (List&lt;AdminUserRow&gt;), q, status
 * POST /admin/users  field: id, action = ban | unban
 */
@WebServlet("/admin/users")
public class AdminUserServlet extends AdminBaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            req.setAttribute("users", adminService.users(req.getParameter("q"), req.getParameter("status")));
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        req.setAttribute("q", req.getParameter("q"));
        req.setAttribute("status", req.getParameter("status"));
        render(req, resp, "users.jsp", "users");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String id = req.getParameter("id");
        handle(req, resp, "/admin/users" + query(req, "q", "status"), () -> {
            switch (String.valueOf(req.getParameter("action"))) {
                case "ban":
                    adminService.banUser(id, adminId(req));
                    return "Đã khoá tài khoản và ẩn các bài đang bán của người này";
                case "unban":
                    adminService.unbanUser(id);
                    return "Đã mở khoá tài khoản";
                default:
                    throw new IllegalArgumentException("Thao tác không hợp lệ");
            }
        });
    }
}
