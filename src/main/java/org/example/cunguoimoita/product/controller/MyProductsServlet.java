package org.example.cunguoimoita.product.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.cunguoimoita.common.util.CurrentUser;
import org.example.cunguoimoita.common.util.FormUtil;
import org.example.cunguoimoita.product.model.Product;
import org.example.cunguoimoita.product.service.ProductService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/** Bài đăng của tôi: lọc theo tab, tạm ẩn, hiện lại, xoá. */
@WebServlet("/product/mine")
public class MyProductsServlet extends HttpServlet {

    /** Các tab và điều kiện lọc của từng tab, theo thứ tự hiện trên trang. */
    private static final Map<String, Predicate<Product>> TABS = new LinkedHashMap<>();

    static {
        TABS.put("all", p -> true);
        TABS.put("pending", Product::isPending);
        TABS.put("selling", Product::isListed);
        TABS.put("paused", p -> p.isPaused() || "hidden".equals(p.getStateCode()));
        TABS.put("done", p -> "sold".equals(p.getStateCode()) || "exchanged".equals(p.getStateCode()));
        TABS.put("rejected", p -> "rejected".equals(p.getStateCode()));
    }

    private final ProductService productService = new ProductService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!allowed(req, resp)) {
            return;
        }
        String tab = TABS.containsKey(req.getParameter("tab")) ? req.getParameter("tab") : "all";
        try {
            List<Product> all = productService.mine(CurrentUser.id(req));
            Map<String, Long> counts = new LinkedHashMap<>();
            TABS.forEach((key, test) -> counts.put(key, all.stream().filter(test).count()));
            req.setAttribute("products", all.stream().filter(TABS.get(tab)).toList());
            req.setAttribute("tabCounts", counts);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        req.setAttribute("tab", tab);
        req.setAttribute("message", req.getParameter("message"));
        req.setAttribute("error", req.getParameter("error"));
        req.getRequestDispatcher("/WEB-INF/views/product/mine.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        if (!allowed(req, resp)) {
            return;
        }
        String sellerId = CurrentUser.id(req);
        String id = req.getParameter("id");
        String back = req.getContextPath() + FormUtil.safeBack(req, "/product/mine");
        String sep = back.contains("?") ? "&" : "?";
        try {
            String action = String.valueOf(req.getParameter("action"));
            switch (action) {
                case "pause":
                    productService.pause(sellerId, id);
                    back += sep + "message=" + FormUtil.encode("Đã ẩn bài, khách sẽ không thấy bài này nữa");
                    break;
                case "resume":
                    productService.resume(sellerId, id);
                    back += sep + "message=" + FormUtil.encode("Bài đã hiện lại trên bảng tin");
                    break;
                case "delete":
                    productService.delete(sellerId, id);
                    back = req.getContextPath() + "/product/mine?message=" + FormUtil.encode("Đã xoá bài");
                    break;
                default:
                    throw new IllegalArgumentException("Thao tác không hợp lệ");
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            back += sep + "error=" + FormUtil.encode(e.getMessage());
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        resp.sendRedirect(back);
    }

    private boolean allowed(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (CurrentUser.id(req) == null) {
            CurrentUser.redirectToLogin(req, resp);
            return false;
        }
        if (!FormUtil.isCustomer(req)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Tài khoản admin không có bài đăng");
            return false;
        }
        return true;
    }
}
