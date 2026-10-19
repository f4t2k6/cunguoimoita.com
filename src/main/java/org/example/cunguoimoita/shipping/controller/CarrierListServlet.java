package org.example.cunguoimoita.shipping.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.cunguoimoita.shipping.service.ShippingService;

import java.io.IOException;
import java.sql.SQLException;

/**
 * GET  /shipping/carriers  → danh sách đơn vị vận chuyển, kèm phí và số ngày dự kiến.
 *      Tham số tuỳ chọn: from, to (tỉnh/thành) để tính phí.
 *      Attribute gửi sang JSP: carriers (List&lt;ShippingCarrier&gt;), from, to, error
 * Người mua chọn hãng ngay trên trang chi tiết đơn (/order/detail), trang này chỉ để tra cứu.
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
}
