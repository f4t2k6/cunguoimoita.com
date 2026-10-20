package org.example.cunguoimoita.dispute.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.cunguoimoita.common.util.CurrentUser;
import org.example.cunguoimoita.dispute.service.PayoutService;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;

/** Thu nhập của người bán và file CSV đối soát. */
@WebServlet(name = "SellerPayoutServlet", urlPatterns = {"/seller/payouts", "/seller/payouts/export"})
public class SellerPayoutServlet extends HttpServlet {

    private final PayoutService payoutService = new PayoutService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String sellerId = CurrentUser.id(req);
        if (sellerId == null) {
            CurrentUser.redirectToLogin(req, resp);
            return;
        }
        try {
            if ("/seller/payouts/export".equals(req.getServletPath())) {
                resp.setContentType("text/csv; charset=UTF-8");
                resp.setHeader("Content-Disposition", "attachment; filename=\"thu-nhap-" + sellerId + ".csv\"");
                try (PrintWriter out = resp.getWriter()) {
                    payoutService.exportCsv(out, sellerId);
                }
                return;
            }
            req.setAttribute("payouts", payoutService.getPayoutsBySeller(sellerId));
            req.setAttribute("summary", payoutService.getSellerSummary(sellerId));
            req.setAttribute("info", payoutService.getSellerInfo(sellerId));
        } catch (SQLException ex) {
            throw new ServletException(ex);
        }
        req.getRequestDispatcher("/WEB-INF/views/dispute/seller-payout.jsp").forward(req, resp);
    }
}
