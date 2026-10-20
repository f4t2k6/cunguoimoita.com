package org.example.cunguoimoita.admin.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/** Duyệt, từ chối, ẩn và hiện lại bài đăng. */
@WebServlet("/admin/products")
public class AdminProductServlet extends AdminBaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String filter = req.getParameter("filter");
        if (filter == null || !filter.matches("pending|approved|hidden|rejected|all")) {
            filter = "pending";
        }
        try {
            req.setAttribute("products", adminService.products(filter, req.getParameter("q")));
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        req.setAttribute("filter", filter);
        req.setAttribute("q", req.getParameter("q"));
        render(req, resp, "products.jsp", "products");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String id = req.getParameter("id");
        String reason = req.getParameter("reason");
        handle(req, resp, "/admin/products" + query(req, "filter", "q"), () -> {
            switch (String.valueOf(req.getParameter("action"))) {
                case "approve":
                    adminService.approveProduct(id, adminId(req));
                    return "Đã duyệt bài, bài đã lên bảng tin";
                case "reject":
                    adminService.rejectProduct(id, reason);
                    return "Đã từ chối bài và báo lý do cho người bán";
                case "hide":
                    adminService.setProductHidden(id, true, reason);
                    return "Đã ẩn bài";
                case "show":
                    adminService.setProductHidden(id, false, null);
                    return "Đã hiện lại bài";
                default:
                    throw new IllegalArgumentException("Thao tác không hợp lệ");
            }
        });
    }
}
