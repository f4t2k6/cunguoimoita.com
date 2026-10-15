package org.example.cunguoimoita.admin.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/**
 * GET  /admin/tiers   hạng thành viên. Attribute gửi sang JSP: tiers (List&lt;MemberTier&gt;)
 * POST /admin/tiers   field: id, minEcoPoints, discountPercent, benefitNote
 */
@WebServlet("/admin/tiers")
public class AdminTierServlet extends AdminBaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            req.setAttribute("tiers", adminService.tiers());
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        render(req, resp, "tiers.jsp", "tiers");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        handle(req, resp, "/admin/tiers", () -> {
            adminService.updateTier(req.getParameter("id"), req.getParameter("minEcoPoints"),
                    req.getParameter("discountPercent"), req.getParameter("benefitNote"));
            return "Đã lưu hạng thành viên";
        });
    }
}
