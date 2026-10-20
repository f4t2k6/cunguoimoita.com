package org.example.cunguoimoita.admin.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/** Quản lý đơn vị vận chuyển: thêm/sửa và bật/tắt hãng. */
@WebServlet("/admin/carriers")
public class AdminCarrierServlet extends AdminBaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            req.setAttribute("carriers", adminService.carriers());
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        render(req, resp, "carriers.jsp", "carriers");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String id = req.getParameter("id");
        boolean active = "true".equals(req.getParameter("active")) || "on".equals(req.getParameter("active"));
        handle(req, resp, "/admin/carriers", () -> {
            if ("toggle".equals(req.getParameter("action"))) {
                adminService.setCarrierActive(id, active);
                return active ? "Đã bật đơn vị vận chuyển" : "Đã tắt đơn vị vận chuyển";
            }
            adminService.saveCarrier(id, req.getParameter("name"), req.getParameter("hotline"),
                    req.getParameter("logoUrl"), req.getParameter("baseFee"), req.getParameter("estimatedDays"), active);
            return id == null || id.isBlank() ? "Đã thêm đơn vị vận chuyển" : "Đã lưu đơn vị vận chuyển";
        });
    }
}
