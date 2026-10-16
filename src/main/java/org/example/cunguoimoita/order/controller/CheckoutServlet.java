package org.example.cunguoimoita.order.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.cunguoimoita.common.util.CurrentUser;
import org.example.cunguoimoita.order.model.Order;
import org.example.cunguoimoita.order.service.OrderService;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;

// POST /order/checkout → bấm "Đặt hàng" ở trang giỏ: tách giỏ thành mỗi người bán 1 đơn.
//      Thành công: 1 đơn → mở trang chi tiết đơn đó; nhiều đơn → mở danh sách đơn mua.
//      Lỗi (giỏ trống, món đã có người đặt...) → quay lại giỏ kèm thông báo.
@WebServlet("/order/checkout")
public class CheckoutServlet extends HttpServlet {

    private final OrderService orderService = new OrderService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String userId = CurrentUser.id(req);
        if (userId == null) {
            CurrentUser.redirectToLogin(req, resp);
            return;
        }
        String ctx = req.getContextPath();
        try {
            List<Order> orders = orderService.checkout(userId);
            if (orders.size() == 1) {
                resp.sendRedirect(ctx + "/order/detail?id=" + orders.get(0).getId());
            } else {
                String msg = "Đã tạo " + orders.size() + " đơn (mỗi người bán 1 đơn)";
                resp.sendRedirect(ctx + "/order/list?as=buyer&message=" + encode(msg));
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            resp.sendRedirect(ctx + "/order/cart?error=" + encode(e.getMessage()));
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}