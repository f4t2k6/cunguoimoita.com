package org.example.cunguoimoita.payment.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.cunguoimoita.common.util.CurrentUser;
import org.example.cunguoimoita.order.dao.OrderDAO;
import org.example.cunguoimoita.payment.model.Payment;
import org.example.cunguoimoita.payment.service.PaymentService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * GET /payment/history → Lịch sử thanh toán của người đang đăng nhập (mọi đơn), mở từ menu Cài đặt.
 *     JSP nhận: rows (List&lt;Row&gt;: payment + mã đơn), mới nhất trước, tối đa 200 dòng.
 */
@WebServlet("/payment/history")
public class PaymentHistoryServlet extends HttpServlet {

    private static final int LIMIT = 200;
    private final PaymentService paymentService = new PaymentService();

    /** Một dòng lịch sử: khoản thanh toán + mã đơn để hiện và dẫn tới trang đơn. */
    public record Row(Payment payment, String orderCode) {
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String userId = CurrentUser.id(req);
        if (userId == null) {
            CurrentUser.redirectToLogin(req, resp);
            return;
        }
        try {
            List<Row> rows = new ArrayList<>();
            for (Payment p : paymentService.getRecentPayments(userId, LIMIT)) {
                rows.add(new Row(p, p.getOrderId() == null ? "" : OrderDAO.codeFor(p.getOrderId())));
            }
            req.setAttribute("rows", rows);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        req.getRequestDispatcher("/WEB-INF/views/payment/history.jsp").forward(req, resp);
    }
}
