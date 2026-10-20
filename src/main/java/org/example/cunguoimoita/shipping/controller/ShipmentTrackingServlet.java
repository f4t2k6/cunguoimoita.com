package org.example.cunguoimoita.shipping.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.cunguoimoita.common.dao.OrderRefDAO.OrderRef;
import org.example.cunguoimoita.common.util.CurrentUser;
import org.example.cunguoimoita.shipping.model.Shipment;
import org.example.cunguoimoita.shipping.model.ShipmentStatus;
import org.example.cunguoimoita.shipping.service.ShippingService;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

/** Theo dõi vận đơn; người bán (hoặc admin) xác nhận bàn giao và bấm các nút giả lập trạng thái. */
@WebServlet("/shipping/track")
public class ShipmentTrackingServlet extends HttpServlet {

    private static final ShipmentStatus[] STEPS = {
            ShipmentStatus.CREATED, ShipmentStatus.PICKED_UP, ShipmentStatus.IN_TRANSIT, ShipmentStatus.DELIVERED
    };

    private final ShippingService shippingService = new ShippingService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String userId = CurrentUser.id(req);
        if (userId == null) {
            CurrentUser.redirectToLogin(req, resp);
            return;
        }
        String orderId = req.getParameter("orderId");
        try {
            OrderRef order = shippingService.requireAccess(orderId, userId, CurrentUser.isAdmin(req), false);
            // Người mua xem vận chuyển ở trang chi tiết đơn (thanh 4 bước + mã vận đơn);
            // thông báo cũ còn trỏ về đây cũng được chuyển sang đó.
            if (userId.equals(order.buyerId()) && !CurrentUser.isAdmin(req)) {
                resp.sendRedirect(req.getContextPath() + "/order/detail?id="
                        + java.net.URLEncoder.encode(orderId, java.nio.charset.StandardCharsets.UTF_8));
                return;
            }
            Shipment shipment = shippingService.findByOrder(orderId);
            if (shipment == null) {
                req.setAttribute("error", "Không tìm thấy vận đơn cho đơn hàng này");
            }
            req.setAttribute("shipment", shipment);
            req.setAttribute("orderCode", order.code());
            req.setAttribute("canManage", CurrentUser.isAdmin(req) || userId.equals(order.sellerId()));
            req.setAttribute("isAdmin", CurrentUser.isAdmin(req));
        } catch (IllegalArgumentException | SecurityException e) {
            req.setAttribute("error", e.getMessage());
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
        String userId = CurrentUser.id(req);
        if (userId == null) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Cần đăng nhập");
            return;
        }
        String orderId = req.getParameter("orderId");
        String action = req.getParameter("action");
        String redirect = req.getContextPath() + "/shipping/track?orderId=" + encode(orderId);
        if ("handover".equals(action)) {
            redirect = req.getContextPath() + "/order/detail?id=" + encode(orderId);
        }
        try {
            shippingService.requireAccess(orderId, userId, CurrentUser.isAdmin(req), true);
            switch (action == null ? "" : action) {
                case "handover": {
                    String h = req.getParameter("handover");
                    ShippingService.Handover method = "pickup".equals(h) ? ShippingService.Handover.PICKUP
                            : "dropoff".equals(h) ? ShippingService.Handover.DROPOFF : null;
                    String where = shippingService.confirmHandover(orderId, userId, method, req.getParameter("addressId"));
                    resp.sendRedirect(redirect + "&message=" + encode("Đã xác nhận giao hàng: " + where));
                    return;
                }
                case "ship":
                    shippingService.markShipped(orderId, req.getParameter("trackingCode"));
                    break;
                case "status": {
                    ShipmentStatus next = ShipmentStatus.fromDb(req.getParameter("status"));
                    // Hoàn hàng là yêu cầu của người mua (Trả hàng/Hoàn tiền), người bán không tự bấm được
                    if (next == ShipmentStatus.RETURNED && !CurrentUser.isAdmin(req)) {
                        throw new SecurityException("Người bán không thể tự hoàn hàng");
                    }
                    shippingService.updateStatus(orderId, next);
                    break;
                }
                case "cod_collected":
                    shippingService.confirmCodCollected(orderId);
                    break;
                case "cod_remitted":
                    shippingService.confirmCodRemitted(orderId);
                    break;
                default:
                    throw new IllegalArgumentException("Thao tác không hợp lệ");
            }
        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
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
