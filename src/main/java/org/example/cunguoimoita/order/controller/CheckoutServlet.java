package org.example.cunguoimoita.order.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.cunguoimoita.common.util.CurrentUser;
import org.example.cunguoimoita.common.util.FormUtil;
import org.example.cunguoimoita.order.model.Order;
import org.example.cunguoimoita.order.service.CartService;
import org.example.cunguoimoita.order.service.OrderService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

// Đặt hàng từ giỏ (mỗi người bán 1 đơn) hoặc "Mua ngay" một món với giá đang đăng.
@WebServlet("/order/checkout")
public class CheckoutServlet extends HttpServlet {

    private final OrderService orderService = new OrderService();
    private final CartService cartService = new CartService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String userId = CurrentUser.id(req);
        if (userId == null) {
            CurrentUser.redirectToLogin(req, resp);
            return;
        }
        String ctx = req.getContextPath();
        String buyNow = req.getParameter("buyNow");
        try {
            List<String> selected;
            if (buyNow != null && !buyNow.isBlank()) {
                cartService.ensureInCart(userId, buyNow);     // kiểm tra còn bán, không phải đồ của mình
                selected = List.of(buyNow);
            } else {
                String[] ids = req.getParameterValues("productId");
                selected = ids == null ? List.of() : Arrays.asList(ids);
            }
            List<Order> orders = orderService.checkout(userId, selected);
            if (orders.size() == 1) {
                resp.sendRedirect(ctx + "/order/detail?id=" + orders.get(0).getId());
            } else {
                String msg = "Đã tạo " + orders.size() + " đơn (mỗi người bán 1 đơn)";
                resp.sendRedirect(ctx + "/order/list?as=buyer&message=" + FormUtil.encode(msg));
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            resp.sendRedirect(ctx + "/order/cart?error=" + FormUtil.encode(e.getMessage()));
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }
}