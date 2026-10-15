package org.example.cunguoimoita.payment.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.cunguoimoita.payment.model.Payment;
import org.example.cunguoimoita.payment.model.PaymentStatus;
import org.example.cunguoimoita.payment.service.PaymentService;
import org.example.cunguoimoita.payment.service.VNPayConfig;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller tiếp nhận kết quả phản hồi từ Cổng thanh toán (VNPay Return URL & Giả lập kết quả).
 */
@WebServlet(name = "PaymentCallbackServlet", urlPatterns = {"/payment/vnpay-return", "/payment/result", "/payment/callback"})
public class PaymentCallbackServlet extends HttpServlet {

    private final PaymentService paymentService = new PaymentService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String servletPath = req.getServletPath();

        // 1. Xử lý Return URL tự động từ Cổng VNPay thật
        if ("/payment/vnpay-return".equals(servletPath)) {
            Map<String, String> fields = new HashMap<>();
            for (Enumeration<String> params = req.getParameterNames(); params.hasMoreElements(); ) {
                String fieldName = params.nextElement();
                String fieldValue = req.getParameter(fieldName);
                if (fieldValue != null && fieldValue.length() > 0) {
                    fields.put(fieldName, fieldValue);
                }
            }

            String vnp_SecureHash = req.getParameter("vnp_SecureHash");
            fields.remove("vnp_SecureHashType");
            fields.remove("vnp_SecureHash");

            boolean isValidSignature = VNPayConfig.verifySignature(fields, vnp_SecureHash);
            String vnp_ResponseCode = req.getParameter("vnp_ResponseCode");
            String vnp_TxnRef = req.getParameter("vnp_TxnRef"); // Payment code
            String vnp_TransactionNo = req.getParameter("vnp_TransactionNo");

            try {
                Payment p = paymentService.getByCode(vnp_TxnRef);
                if (p != null) {
                    if (isValidSignature && "00".equals(vnp_ResponseCode)) {
                        paymentService.markPaid(p.getId(), vnp_TransactionNo);
                        req.setAttribute("payStatus", "paid");
                    } else {
                        paymentService.markFailed(p.getId());
                        req.setAttribute("payStatus", "failed");
                    }
                    req.setAttribute("payment", p);
                    req.setAttribute("orderCode", p.getOrderId());
                    req.setAttribute("gateway", "vnpay");
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }

            req.setAttribute("currentPage", "payment-result");
            req.getRequestDispatcher("/WEB-INF/views/payment/payment-result.jsp").forward(req, resp);
            return;
        }

        // 2. Xử lý hiển thị kết quả và hỗ trợ nút bấm Test Demo kịch bản
        String paymentId = req.getParameter("paymentId");
        String status = req.getParameter("status");
        String orderCode = req.getParameter("orderCode");
        String gateway = req.getParameter("gateway");

        if (status == null || status.isBlank()) {
            status = "paid";
        }
        if (gateway == null || gateway.isBlank()) {
            gateway = "vnpay";
        }
        if (orderCode == null || orderCode.isBlank()) {
            orderCode = "ORD-2026-0889";
        }

        try {
            if (paymentId != null && !paymentId.isBlank()) {
                Payment p = paymentService.getById(paymentId);
                if (p != null) {
                    if ("paid".equalsIgnoreCase(status)) {
                        paymentService.markPaid(paymentId, "TXN-" + System.currentTimeMillis() % 100000);
                    } else if ("failed".equalsIgnoreCase(status)) {
                        paymentService.markFailed(paymentId);
                    }
                    req.setAttribute("payment", p);
                    req.setAttribute("orderCode", p.getOrderId());
                    req.setAttribute("gateway", p.getGateway().dbValue());
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        req.setAttribute("payStatus", status);
        req.setAttribute("gateway", gateway);
        req.setAttribute("orderCode", orderCode);
        req.setAttribute("currentPage", "payment-result");
        req.getRequestDispatcher("/WEB-INF/views/payment/payment-result.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }
}
