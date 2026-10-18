package org.example.cunguoimoita.product.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.cunguoimoita.common.util.CurrentUser;
import org.example.cunguoimoita.feed.service.FeedService;
import org.example.cunguoimoita.product.model.Product;
import org.example.cunguoimoita.product.service.ProductService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * GET /product?id=  → trang chi tiết bài đăng (F3): ảnh lớn, thông tin, người bán,
 *                     nút Lưu / Nhắn tin / Thêm vào giỏ, gợi ý trao đổi (F2) và bài khác của người bán.
 * JSP nhận: product, isOwner, matches (List<Product>), sellerOthers (List<Product>), message, error
 */
@WebServlet("/product")
public class ProductDetailServlet extends HttpServlet {

    private final ProductService productService = new ProductService();
    private final FeedService feedService = new FeedService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String viewerId = CurrentUser.id(req);
        try {
            Product product = productService.detail(req.getParameter("id"), viewerId, CurrentUser.isAdmin(req));
            if (product == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Bài đăng không tồn tại hoặc đã bị gỡ");
                return;
            }
            product.setSaved(feedService.isSaved(viewerId, product.getId()));

            List<Product> matches = productService.suggestMatches(product, 6);
            List<Product> others = productService.publicOfSeller(product.getSellerId(), 5).stream()
                    .filter(p -> !p.getId().equals(product.getId()))
                    .limit(4)
                    .toList();
            feedService.markSaved(matches, viewerId);

            req.setAttribute("product", product);
            req.setAttribute("isOwner", product.getSellerId().equals(viewerId));
            req.setAttribute("matches", matches);
            req.setAttribute("sellerOthers", others);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        req.setAttribute("message", req.getParameter("message"));
        req.setAttribute("error", req.getParameter("error"));
        req.getRequestDispatcher("/WEB-INF/views/product/view.jsp").forward(req, resp);
    }
}
