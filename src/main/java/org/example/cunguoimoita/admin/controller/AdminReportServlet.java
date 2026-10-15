package org.example.cunguoimoita.admin.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/**
 * GET  /admin/reports?status=pending|resolved|rejected|all   danh sách báo cáo vi phạm (mặc định: chờ xử lý)
 *      Attribute gửi sang JSP: reports (List&lt;Report&gt;), status
 * POST /admin/reports  field: id, action = resolve (kèm do = none | hide_product | ban_user) | reject
 */
@WebServlet("/admin/reports")
public class AdminReportServlet extends AdminBaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String status = req.getParameter("status");
        if (status == null || !status.matches("pending|resolved|rejected|all")) {
            status = "pending";
        }
        try {
            req.setAttribute("reports", adminService.reports(status));
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        req.setAttribute("status", status);
        render(req, resp, "reports.jsp", "reports");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String id = req.getParameter("id");
        handle(req, resp, "/admin/reports" + query(req, "status"), () -> {
            switch (String.valueOf(req.getParameter("action"))) {
                case "resolve":
                    adminService.resolveReport(id, req.getParameter("do"), adminId(req));
                    return "Đã xử lý báo cáo và báo cho người gửi";
                case "reject":
                    adminService.rejectReport(id, adminId(req));
                    return "Đã bác bỏ báo cáo";
                default:
                    throw new IllegalArgumentException("Thao tác không hợp lệ");
            }
        });
    }
}
