package org.example.cunguoimoita.payment.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.cunguoimoita.common.util.CurrentUser;
import org.example.cunguoimoita.order.model.Order;
import org.example.cunguoimoita.order.service.OrderService;
import org.example.cunguoimoita.payment.model.Payment;
import org.example.cunguoimoita.payment.model.PaymentGateway;
import org.example.cunguoimoita.payment.service.PaymentService;
import org.example.cunguoimoita.payment.service.VNPayConfig;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

/** Chọn cổng thanh toán online; số tiền luôn lấy từ tổng đơn trong DB. */
@WebServlet(name = "PaymentCheckoutServlet", urlPatterns = {"/payment/checkout"})
public class PaymentCheckoutServlet extends HttpServlet {

    private final PaymentService paymentService = new PaymentService();
    private final OrderService orderService = new OrderService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String userId = CurrentUser.id(req);
        if (userId == null) {
            CurrentUser.redirectToLogin(req, resp);
            return;
        }
        String orderId = req.getParameter("orderId");
        Order order;
        try {
            order = orderService.getOrder(orderId, userId);
        } catch (IllegalArgumentException | SecurityException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        if (!userId.equals(order.getBuyerId()) || order.getStatus() != Order.Status.AWAITING_PAYMENT) {
            // Đơn đã trả rồi / đang COD / không phải đơn của mình → về trang đơn
            resp.sendRedirect(req.getContextPath() + "/order/detail?id=" + encode(orderId)
                    + "&error=" + encode("Đơn này không ở bước thanh toán online"));
            return;
        }
        req.setAttribute("order", order);
        req.setAttribute("gateways", PaymentGateway.values());
        req.setAttribute("vnpayReal", VNPayConfig.isConfigured());
        req.setAttribute("error", req.getParameter("error"));
        req.getRequestDispatcher("/WEB-INF/views/payment/checkout-gateway.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String userId = CurrentUser.id(req);
        if (userId == null) {
            CurrentUser.redirectToLogin(req, resp);
            return;
        }
        String orderId = req.getParameter("orderId");
        PaymentGateway gateway = PaymentGateway.fromDb(req.getParameter("gateway"));
        try {
            Payment p = paymentService.startPayment(orderId, userId, gateway);

            if (gateway == PaymentGateway.VNPAY) {
                VNPayConfig.init(getServletContext());
                String ipAddr = req.getRemoteAddr();
                if ("0:0:0:0:0:0:0:1".equals(ipAddr) || "localhost".equals(ipAddr) || ipAddr == null) {
                    ipAddr = "127.0.0.1";
                }
                String returnUrl = req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort()
                                 + req.getContextPath() + "/payment/vnpay-return";
                String paymentUrl = VNPayConfig.buildPaymentUrl(p.getCode(), Math.round(p.getAmount()),
                        "Thanh toan don hang " + p.getCode(), ipAddr, returnUrl);
                resp.sendRedirect(paymentUrl);
                return;
            }
            // Các cổng khác (MoMo / Bank Transfer chưa nối API thật) → chuyển sang sandbox nội bộ
            resp.sendRedirect(req.getContextPath() + "/payment/sandbox?paymentId=" + encode(p.getId()));
        } catch (IllegalStateException | SecurityException e) {
            resp.sendRedirect(req.getContextPath() + "/order/detail?id=" + encode(orderId) + "&error=" + encode(e.getMessage()));
        } catch (SQLException ex) {
            throw new ServletException(ex);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
