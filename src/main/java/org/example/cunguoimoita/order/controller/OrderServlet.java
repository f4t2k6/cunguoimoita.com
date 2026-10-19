package org.example.cunguoimoita.order.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.cunguoimoita.common.util.CurrentUser;
import org.example.cunguoimoita.dispute.service.DisputeService;
import org.example.cunguoimoita.dispute.service.PayoutService;
import org.example.cunguoimoita.dispute.service.RefundService;
import org.example.cunguoimoita.order.model.Order;
import org.example.cunguoimoita.order.service.OrderService;
import org.example.cunguoimoita.payment.service.PaymentService;
import org.example.cunguoimoita.shipping.model.Shipment;
import org.example.cunguoimoita.shipping.service.ShippingService;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

// GET  /order/list?as=buyer|seller → "Đơn mua của tôi" / "Đơn bán của tôi" (order-list.jsp)
//      Gửi sang JSP: orders (List<Order>), as, message
// GET  /order/detail?id=...         → chi tiết + thanh toán 1 đơn (order-detail.jsp)
//      Gửi sang JSP: order, isBuyer, isSeller, shipment, payments, dispute, refund, payout, error, message
//      + khi người mua còn sửa được đơn: addresses, quotes (hãng + phí ship), vouchers, codProblem
// POST /order/detail                → các nút trên trang chi tiết. Field: id, action, + dữ liệu của action
//      action = address  (addressId)   người mua chọn địa chỉ giao (phí ship tính lại)
//               carrier  (carrierId)   người mua chọn đơn vị vận chuyển → phí ship cộng vào tổng
//               voucher  (code)        người mua áp mã giảm giá
//               payment  (method)      người mua chọn cod (xác nhận đặt hàng) / online (sang trang thanh toán F5)
//               cancel   (reason)      người mua hoặc người bán huỷ đơn
//               received               người mua bấm "Đã nhận hàng"
@WebServlet(urlPatterns = {"/order/list", "/order/detail"})
public class OrderServlet extends HttpServlet {

    private final OrderService orderService = new OrderService();
    private final PaymentService paymentService = new PaymentService();
    private final DisputeService disputeService = new DisputeService();
    private final RefundService refundService = new RefundService();
    private final PayoutService payoutService = new PayoutService();
    private final ShippingService shippingService = new ShippingService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String userId = CurrentUser.id(req);
        if (userId == null) {
            CurrentUser.redirectToLogin(req, resp);
            return;
        }
        req.setAttribute("error", req.getParameter("error"));
        req.setAttribute("message", req.getParameter("message"));
        try {
            if ("/order/list".equals(req.getServletPath())) {      // 1 Servlet phục vụ 2 URL → xem URL nào được gọi
                showList(req, resp, userId);
            } else {
                showDetail(req, resp, userId);
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String userId = CurrentUser.id(req);
        if (userId == null) {
            CurrentUser.redirectToLogin(req, resp);
            return;
        }
        String orderId = req.getParameter("id");
        String action = req.getParameter("action");
        String redirect = req.getContextPath() + "/order/detail?id=" + encode(orderId);
        try {
            switch (action == null ? "" : action) {
                case "address":
                    orderService.chooseAddress(orderId, userId, req.getParameter("addressId"));
                    break;
                case "carrier":
                    orderService.chooseCarrier(orderId, userId, req.getParameter("carrierId"));
                    break;
                case "voucher":
                    orderService.applyVoucher(orderId, userId, req.getParameter("code"));
                    break;
                case "payment": {
                    Order.PaymentMethod method = parseMethod(req.getParameter("method"));
                    orderService.choosePaymentMethod(orderId, userId, method);
                    if (method == Order.PaymentMethod.ONLINE) {      // sang trang chọn cổng của F5
                        resp.sendRedirect(req.getContextPath() + "/payment/checkout?orderId=" + encode(orderId));
                        return;
                    }
                    break;
                }
                case "cancel":
                    orderService.cancel(orderId, userId, req.getParameter("reason"));
                    break;
                case "received":
                    orderService.confirmReceived(orderId, userId);
                    break;
                default:
                    throw new IllegalArgumentException("Thao tác không hợp lệ");
            }
            redirect += "&message=" + encode("Đã cập nhật đơn hàng");
        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            redirect += "&error=" + encode(e.getMessage());
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        resp.sendRedirect(redirect);
    }

    // Danh sách đơn mua hoặc đơn bán.
    private void showList(HttpServletRequest req, HttpServletResponse resp, String userId)
            throws SQLException, ServletException, IOException {
        boolean asSeller = "seller".equals(req.getParameter("as"));
        req.setAttribute("as", asSeller ? "seller" : "buyer");
        req.setAttribute("orders", asSeller ? orderService.getSellerOrders(userId) : orderService.getBuyerOrders(userId));
        req.getRequestDispatcher("/WEB-INF/views/order/order-list.jsp").forward(req, resp);
    }

    // Chi tiết 1 đơn. Không có quyền hoặc không tìm thấy → 404.
    private void showDetail(HttpServletRequest req, HttpServletResponse resp, String userId)
            throws SQLException, ServletException, IOException {
        Order order;
        try {
            order = orderService.getOrder(req.getParameter("id"), userId);
        } catch (IllegalArgumentException | SecurityException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        boolean isBuyer = userId.equals(order.getBuyerId());
        Shipment shipment = orderService.getShipment(order.getId());
        // Đã xác nhận COD = đơn còn "new" nhưng vận đơn đang chờ thu tiền → người mua không sửa được nữa
        boolean codConfirmed = shipment != null && shipment.isCod();
        boolean editable = (order.getStatus() == Order.Status.NEW || order.getStatus() == Order.Status.AWAITING_PAYMENT)
                && !codConfirmed;
        req.setAttribute("order", order);
        req.setAttribute("isBuyer", isBuyer);
        req.setAttribute("isSeller", !isBuyer);
        req.setAttribute("shipment", shipment);
        req.setAttribute("carrierName", shipment == null ? null : shippingService.carrierName(shipment.getCarrierId()));
        req.setAttribute("codConfirmed", codConfirmed);
        req.setAttribute("editable", editable);
        req.setAttribute("payments", paymentService.getHistory(order.getId()));
        req.setAttribute("dispute", disputeService.getByOrderId(order.getId()));
        req.setAttribute("refund", refundService.getByOrderId(order.getId()));
        if (!isBuyer) {
            req.setAttribute("payout", payoutService.getByOrderId(order.getId()));
        }
        if (isBuyer && editable) {
            req.setAttribute("addresses", orderService.getBuyerAddresses(userId));
            req.setAttribute("quotes", orderService.getCarrierQuotes(order.getId()));
            req.setAttribute("vouchers", orderService.getUsableVouchers(userId));
            req.setAttribute("codProblem", orderService.getCodProblem(order.getId(), userId));
        }
        req.getRequestDispatcher("/WEB-INF/views/order/order-detail.jsp").forward(req, resp);
    }

    // "online" → ONLINE, "cod" → COD; chữ lạ → báo lỗi.
    private Order.PaymentMethod parseMethod(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Vui lòng chọn phương thức thanh toán");
        }
        return Order.PaymentMethod.valueOf(value.toUpperCase());   // chữ lạ: valueOf ném IllegalArgumentException
    }

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}