package org.example.cunguoimoita.shipping.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.cunguoimoita.shipping.model.Shipment;
import org.example.cunguoimoita.shipping.model.ShipmentStatus;
import org.example.cunguoimoita.shipping.service.ShippingService;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

/**
 * GET  /shipping/track?orderId=...  → trang theo dõi đơn (dòng thời gian trạng thái).
 *      Attribute gửi sang JSP: shipment (Shipment), steps (ShipmentStatus[]), error
 * POST /shipping/track  → các nút trên trang theo dõi. Field: orderId, action, trackingCode, status
 *      action = ship          người bán bấm "Đã gửi hàng" (kèm trackingCode)
 *               status        nút giả lập đổi trạng thái (kèm status: in_transit, delivered, returned)
 *               cod_collected shipper đã thu tiền COD
 *               cod_remitted  shipper đã nộp tiền COD về sàn
 * TODO: khi có AuthFilter (TV1), chỉ người bán được bấm "Đã gửi hàng", nút giả lập chỉ để admin.
 */
@WebServlet("/shipping/track")
public class ShipmentTrackingServlet extends HttpServlet {

    private static final ShipmentStatus[] STEPS = {
            ShipmentStatus.CREATED, ShipmentStatus.PICKED_UP, ShipmentStatus.IN_TRANSIT, ShipmentStatus.DELIVERED
    };

    private final ShippingService shippingService = new ShippingService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String orderId = req.getParameter("orderId");
        try {
            Shipment shipment = orderId == null ? null : shippingService.findByOrder(orderId);
            if (shipment == null) {
                req.setAttribute("error", "Không tìm thấy vận đơn cho đơn hàng này");
            }
            req.setAttribute("shipment", shipment);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        req.setAttribute("steps", STEPS);
        String flash = req.getParameter("error");
        if (flash != null) {
            req.setAttribute("error", flash);
        }
        req.getRequestDispatcher("/WEB-INF/views/shipping/tracking.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String orderId = req.getParameter("orderId");
        String action = req.getParameter("action");
        String redirect = req.getContextPath() + "/shipping/track?orderId=" + encode(orderId);
        try {
            switch (action == null ? "" : action) {
                case "ship":
                    shippingService.markShipped(orderId, req.getParameter("trackingCode"));
                    break;
                case "status":
                    shippingService.updateStatus(orderId, ShipmentStatus.fromDb(req.getParameter("status")));
                    break;
                case "cod_collected":
                    shippingService.confirmCodCollected(orderId);
                    break;
                case "cod_remitted":
                    shippingService.confirmCodRemitted(orderId);
                    break;
                default:
                    throw new IllegalArgumentException("Thao tác không hợp lệ");
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect += "&error=" + encode(e.getMessage());
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        resp.sendRedirect(redirect);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
