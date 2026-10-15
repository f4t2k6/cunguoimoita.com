package org.example.cunguoimoita.dispute.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.cunguoimoita.common.util.CurrentUser;
import org.example.cunguoimoita.dispute.dao.PayoutDAO;
import org.example.cunguoimoita.dispute.model.Payout;
import org.example.cunguoimoita.dispute.service.PayoutService;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.List;

/**
 * Controller Quản lý Ví Doanh thu Người bán, Lịch sử Giải ngân Payout và Xuất báo cáo CSV (F9 Payout).
 */
@WebServlet(name = "SellerPayoutServlet", urlPatterns = {"/seller/payouts", "/seller/payouts/export"})
public class SellerPayoutServlet extends HttpServlet {

    private final PayoutService payoutService = new PayoutService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String servletPath = req.getServletPath();
        String sellerId = CurrentUser.id(req);
        if (sellerId == null) {
            sellerId = "S1"; // Mặc định người bán mẫu S1
        }

        // 1. Xuất file CSV (Chương 18 kỹ thuật thiết lập Response Headers)
        if ("/seller/payouts/export".equals(servletPath)) {
            resp.setContentType("text/csv; charset=UTF-8");
            resp.setHeader("Content-Disposition", "attachment; filename=\"payout_report_" + sellerId + ".csv\"");
            try (PrintWriter out = resp.getWriter()) {
                payoutService.exportCsv(out, sellerId);
            } catch (SQLException ex) {
                throw new ServletException(ex);
            }
            return;
        }

        // 2. Hiển thị trang quản lý Payout
        try {
            List<Payout> payouts = payoutService.getPayoutsBySeller(sellerId);
            PayoutDAO.SellerEarningsSummary summary = payoutService.getSellerSummary(sellerId);

            req.setAttribute("payouts", payouts);
            req.setAttribute("summary", summary);
            req.setAttribute("sellerId", sellerId);

            // Tỷ lệ mô phỏng theo hạng
            String tierRate = req.getParameter("tierRate");
            if (tierRate != null && !tierRate.isBlank()) {
                req.setAttribute("tierRate", tierRate);
            } else {
                req.setAttribute("tierRate", "3"); // 3% Gold Tier mặc định
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            req.setAttribute("error", ex.getMessage());
        }

        req.setAttribute("currentPage", "payout");
        req.getRequestDispatcher("/WEB-INF/views/dispute/seller-payout.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }
}
