package org.example.cunguoimoita.payment.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.cunguoimoita.common.util.CurrentUser;
import org.example.cunguoimoita.payment.model.Payment;
import org.example.cunguoimoita.payment.model.PaymentStatus;
import org.example.cunguoimoita.payment.service.PaymentService;
import org.example.cunguoimoita.payment.service.VNPayConfig;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/** Kết quả từ cổng thanh toán: VNPay trả về, trang giả lập sandbox và trang hiển thị kết quả. */
@WebServlet(name = "PaymentCallbackServlet", urlPatterns = {"/payment/vnpay-return", "/payment/result", "/payment/sandbox"})
public class PaymentCallbackServlet extends HttpServlet {

    private final PaymentService paymentService = new PaymentService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            if ("/payment/vnpay-return".equals(req.getServletPath())) {
                handleVnpayReturn(req, resp);
                return;
            }
            String userId = CurrentUser.id(req);
            if (userId == null) {
                CurrentUser.redirectToLogin(req, resp);
                return;
            }
            Payment p = paymentService.requirePayer(req.getParameter("paymentId"), userId);
            req.setAttribute("payment", p);
            if ("/payment/sandbox".equals(req.getServletPath())) {
                if (p.getStatus() != PaymentStatus.PENDING) {
                    resp.sendRedirect(req.getContextPath() + "/payment/result?paymentId=" + encode(p.getId()));
                    return;
                }
                req.getRequestDispatcher("/WEB-INF/views/payment/sandbox.jsp").forward(req, resp);
                return;
            }
            showResult(req, resp, p);
        } catch (SecurityException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String userId = CurrentUser.id(req);
        if (userId == null || !"/payment/sandbox".equals(req.getServletPath())) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String paymentId = req.getParameter("paymentId");
        try {
            paymentService.simulate(paymentId, userId, "success".equals(req.getParameter("result")));
        } catch (SecurityException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        } catch (IllegalStateException ignored) {
            // khoản đã xử lý rồi (bấm 2 lần) → cứ xem kết quả
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        resp.sendRedirect(req.getContextPath() + "/payment/result?paymentId=" + encode(paymentId));
    }

    /** Return URL của VNPay: chữ ký đúng + mã 00 + số tiền khớp → markPaid; còn lại → markFailed. */
    private void handleVnpayReturn(HttpServletRequest req, HttpServletResponse resp)
            throws SQLException, ServletException, IOException {
        VNPayConfig.init(getServletContext());
        Map<String, String> fields = new HashMap<>();
        for (Enumeration<String> params = req.getParameterNames(); params.hasMoreElements(); ) {
            String fieldName = params.nextElement();
            String fieldValue = req.getParameter(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                fields.put(fieldName, fieldValue);
            }
        }
        String secureHash = req.getParameter("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");
        fields.remove("vnp_SecureHash");

        Payment p = paymentService.getByCode(req.getParameter("vnp_TxnRef"));
        if (p == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        boolean signed = VNPayConfig.verifySignature(fields, secureHash);
        boolean amountOk = String.valueOf(Math.round(p.getAmount()) * 100).equals(req.getParameter("vnp_Amount"));
        if (signed && amountOk && "00".equals(req.getParameter("vnp_ResponseCode"))) {
            paymentService.markPaid(p.getId(), req.getParameter("vnp_TransactionNo"));
        } else if (signed) {
            paymentService.markFailed(p.getId());
        }
        showResult(req, resp, p);
    }

    private void showResult(HttpServletRequest req, HttpServletResponse resp, Payment p)
            throws SQLException, ServletException, IOException {
        Payment fresh = paymentService.getById(p.getId());
        req.setAttribute("payment", fresh);
        req.setAttribute("payStatus", fresh.getStatus().dbValue());
        req.getRequestDispatcher("/WEB-INF/views/payment/payment-result.jsp").forward(req, resp);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
