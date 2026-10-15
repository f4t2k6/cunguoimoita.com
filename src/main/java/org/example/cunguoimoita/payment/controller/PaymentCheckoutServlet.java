package org.example.cunguoimoita.payment.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.cunguoimoita.common.dao.OrderRefDAO;
import org.example.cunguoimoita.common.db.ConnectionPool;
import org.example.cunguoimoita.common.util.CurrentUser;
import org.example.cunguoimoita.payment.model.Payment;
import org.example.cunguoimoita.payment.model.PaymentGateway;
import org.example.cunguoimoita.payment.service.PaymentService;
import org.example.cunguoimoita.payment.service.VNPayConfig;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Controller xử lý quy trình thanh toán Escrow và kiểm tra điều kiện COD.
 */
@WebServlet(name = "PaymentCheckoutServlet", urlPatterns = {"/payment/checkout", "/payment/process"})
public class PaymentCheckoutServlet extends HttpServlet {

    private final PaymentService paymentService = new PaymentService();
    private final OrderRefDAO orderDAO = new OrderRefDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String userId = CurrentUser.id(req);
        if (userId == null) {
            userId = "B1"; // Mặc định tài khoản Người Mua mẫu để tiện demo
        }

        String orderId = req.getParameter("orderId");
        if (orderId == null || orderId.isBlank()) {
            orderId = "O1"; // Đơn hàng mẫu O1 trong 00_database_chung.sql
        }

        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            OrderRefDAO.OrderRef order = orderDAO.find(c, orderId);
            if (order != null) {
                req.setAttribute("order", order);
                req.setAttribute("orderCode", order.code());
            } else {
                req.setAttribute("orderCode", "ORD-2026-0889");
            }

            boolean canUseCod = paymentService.canUseCod(userId);
            req.setAttribute("canUseCod", canUseCod);
            req.setAttribute("buyerId", userId);

            // Cho phép thử nghiệm giả lập kịch bản từ query param
            String scenario = req.getParameter("scenario");
            if ("cod_blocked".equals(scenario)) {
                req.setAttribute("canUseCod", false);
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            req.setAttribute("error", "Lỗi kết nối cơ sở dữ liệu: " + ex.getMessage());
        }

        req.setAttribute("currentPage", "payment");
        req.getRequestDispatcher("/WEB-INF/views/payment/checkout-gateway.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String userId = CurrentUser.id(req);
        if (userId == null) {
            userId = "B1";
        }

        String orderId = req.getParameter("orderId");
        if (orderId == null || orderId.isBlank()) {
            orderId = "O1";
        }

        String gatewayStr = req.getParameter("gateway");
        double amount = 1330000;
        try {
            String amountStr = req.getParameter("amount");
            if (amountStr != null && !amountStr.isBlank()) {
                amount = Double.parseDouble(amountStr);
            }
        } catch (NumberFormatException ignored) {
        }

        // Trường hợp người mua chọn COD
        if ("cod".equalsIgnoreCase(gatewayStr)) {
            try {
                boolean canUseCod = paymentService.canUseCod(userId);
                if (!canUseCod) {
                    req.setAttribute("errorMessage", "Tài khoản của bạn đã bị khoá tính năng COD do lịch sử từ chối nhận hàng.");
                    doGet(req, resp);
                    return;
                }

                try (Connection c = ConnectionPool.getInstance().getConnection()) {
                    orderDAO.updateStatus(c, orderId, "new");
                }
                resp.sendRedirect(req.getContextPath() + "/ui?page=shipment&orderId=" + orderId);
                return;
            } catch (SQLException ex) {
                throw new ServletException(ex);
            }
        }

        // Trường hợp thanh toán Online Escrow
        PaymentGateway gateway = PaymentGateway.fromDb(gatewayStr);
        try {
            Payment p = paymentService.createPayment(orderId, userId, amount, gateway);

            // Nếu chọn VNPay Sandbox
            if (gateway == PaymentGateway.VNPAY) {
                String ipAddr = req.getRemoteAddr();
                if ("0:0:0:0:0:0:0:1".equals(ipAddr)) ipAddr = "127.0.0.1";

                String returnUrl = req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort()
                                 + req.getContextPath() + "/payment/vnpay-return";

                String paymentUrl = VNPayConfig.buildPaymentUrl(p.getCode(), (long) amount,
                        "Thanh toan don hang " + p.getOrderId(), ipAddr, returnUrl);

                // Nếu VNPay sandbox hợp lệ, chuyển hướng sang cổng VNPay
                resp.sendRedirect(paymentUrl);
                return;
            }

            // Với VietQR hoặc MoMo: Chuyển hướng tới trang xác nhận
            resp.sendRedirect(req.getContextPath() + "/payment/result?paymentId=" + p.getId() + "&orderCode=" + orderId);

        } catch (SQLException ex) {
            throw new ServletException(ex);
        }
    }
}
