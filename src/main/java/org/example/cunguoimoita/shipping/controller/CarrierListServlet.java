package org.example.cunguoimoita.shipping.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.cunguoimoita.common.util.CurrentUser;
import org.example.cunguoimoita.shipping.model.Shipment;
import org.example.cunguoimoita.shipping.service.ShippingService;

import java.io.IOException;
import java.sql.SQLException;

/**
 * GET  /shipping/carriers  → danh sách đơn vị vận chuyển, kèm phí và số ngày dự kiến.
 *      Tham số tuỳ chọn: from, to (tỉnh/thành) để tính phí.
 *      Attribute gửi sang JSP: carriers (List&lt;ShippingCarrier&gt;), from, to, error
 * POST /shipping/carriers  → TẠM để thử khi F4 (TV3) chưa có trang đặt hàng: người bán tạo vận đơn cho đơn của mình.
 *      Field: orderId, carrierId, from, to, codAmount. Phải đăng nhập và là người bán của đơn (hoặc admin).
 */
@WebServlet("/shipping/carriers")
public class CarrierListServlet extends HttpServlet {

    private final ShippingService shippingService = new ShippingService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            req.setAttribute("carriers", shippingService.listCarriers());
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        req.setAttribute("from", req.getParameter("from"));
        req.setAttribute("to", req.getParameter("to"));
        req.getRequestDispatcher("/WEB-INF/views/shipping/carriers.jsp").forward(req, resp);
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
        try {
            shippingService.requireAccess(orderId, userId, CurrentUser.isAdmin(req), true);
            double codAmount = parseAmount(req.getParameter("codAmount"));
            Shipment s = shippingService.createShipment(orderId, req.getParameter("carrierId"),
                    req.getParameter("from"), req.getParameter("to"), codAmount);
            resp.sendRedirect(req.getContextPath() + "/shipping/track?orderId=" + s.getOrderId());
        } catch (IllegalArgumentException | SecurityException e) {
            req.setAttribute("error", e instanceof NumberFormatException ? "Tiền thu hộ không hợp lệ" : e.getMessage());
            doGet(req, resp);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private double parseAmount(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        double amount = Double.parseDouble(value.trim());
        if (!Double.isFinite(amount) || amount < 0) {
            throw new IllegalArgumentException("Tiền thu hộ không hợp lệ");
        }
        return amount;
    }
}
