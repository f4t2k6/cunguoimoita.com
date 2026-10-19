package org.example.cunguoimoita.admin.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.cunguoimoita.common.ScheduledJobs;
import org.example.cunguoimoita.dispute.service.DisputeService;

import java.io.IOException;
import java.sql.SQLException;

/**
 * GET /admin/dashboard → trang tổng quan: số người dùng, bài đăng, đơn hàng, doanh thu phí, việc đang chờ.
 * Attribute gửi sang JSP: stats (DashboardStats), overview (đơn theo trạng thái, 5 bài chờ duyệt và 5 báo cáo mới nhất),
 * pendingDisputes (khiếu nại đang mở). Quyền được AdminAuthFilter kiểm tra.
 * POST /admin/dashboard → chạy ngay các tác vụ định kỳ (tự hoàn tất đơn sau 7 ngày, chuyển tiền người bán, voucher hết hạn).
 */
@WebServlet("/admin/dashboard")
public class AdminDashboardServlet extends AdminBaseServlet {

    /** Số việc mới nhất hiện ở mỗi khung "Việc cần xử lý". */
    private static final int LATEST = 5;

    private final DisputeService disputeService = new DisputeService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            req.setAttribute("stats", adminService.stats());
            req.setAttribute("overview", adminService.overview(LATEST));
            req.setAttribute("pendingDisputes", disputeService.listPendingForAdmin());
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        render(req, resp, "dashboard.jsp", "dashboard");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handle(req, resp, "/admin/dashboard", () -> {
            try {
                return ScheduledJobs.runNow().summary();
            } catch (SQLException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalStateException("Chạy tác vụ lỗi: " + e.getMessage());
            }
        });
    }
}
