package org.example.cunguoimoita.order.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.cunguoimoita.common.util.CurrentUser;
import org.example.cunguoimoita.order.dao.CartDAO.CartLine;
import org.example.cunguoimoita.order.service.CartService;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

// GET  /order/cart  → hiện trang giỏ hàng (cart.jsp).
//      Gửi sang JSP: lines (List<CartLine>), total (double), error, message
// POST /order/cart  → các nút trên trang. Field: action (add | remove | clear | removeSelected), productId (removeSelected: nhiều productId)
@WebServlet("/order/cart")
public class CartServlet extends HttpServlet {

    private final CartService cartService = new CartService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String userId = CurrentUser.id(req);                 // lấy id người đăng nhập từ session
        if (userId == null) {
            CurrentUser.redirectToLogin(req, resp);          // chưa đăng nhập → sang trang login
            return;
        }
        try {
            List<CartLine> lines = cartService.getLines(userId);
            double total = 0;                                // tổng cả giỏ (trang tự tính lại theo các món được tick)
            for (CartLine line : lines) {
                total += line.price();
            }
            req.setAttribute("lines", lines);                // đặt dữ liệu vào request cho JSP đọc
            req.setAttribute("total", total);
        } catch (SQLException e) {
            throw new ServletException(e);                   // lỗi DB → Tomcat hiện trang lỗi 500
        }
        req.setAttribute("error", req.getParameter("error"));
        req.setAttribute("message", req.getParameter("message"));
        req.getRequestDispatcher("/WEB-INF/views/order/cart.jsp").forward(req, resp);   // chuyển sang JSP
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");                   // đọc đúng tiếng Việt trong form
        String userId = CurrentUser.id(req);
        if (userId == null) {
            CurrentUser.redirectToLogin(req, resp);
            return;
        }
        String action = req.getParameter("action");
        String productId = req.getParameter("productId");
        String redirect = req.getContextPath() + "/order/cart";
        try {
            switch (action == null ? "add" : action) {
                case "add":
                    cartService.addToCart(userId, productId);
                    redirect += "?message=" + encode("Đã thêm vào giỏ");
                    break;
                case "remove":
                    cartService.removeFromCart(userId, productId);
                    break;
                case "clear":
                    cartService.clearCart(userId);
                    break;
                case "removeSelected": {     // nút "Xoá mục đã chọn" (các ô tick productId)
                    String[] ids = req.getParameterValues("productId");
                    cartService.removeFromCart(userId, ids == null ? List.of() : Arrays.asList(ids));
                    break;
                }
                default:
                    throw new IllegalArgumentException("Thao tác không hợp lệ");
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect += "?error=" + encode(e.getMessage());  // lỗi nghiệp vụ → hiện lên trang giỏ
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        resp.sendRedirect(redirect);                         // POST xong → redirect (PRG), tránh F5 gửi lại form
    }

    // Mã hoá chữ để đặt được lên URL (dấu cách, tiếng Việt...)
    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}