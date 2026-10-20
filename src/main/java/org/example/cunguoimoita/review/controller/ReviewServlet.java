package org.example.cunguoimoita.review.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.cunguoimoita.common.util.CurrentUser;
import org.example.cunguoimoita.review.model.Review;
import org.example.cunguoimoita.review.service.ReviewService;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

// GET  /review/create?orderId=...  → form đánh giá (review_form.jsp). Gửi sang JSP: orderId, error
// POST /review/create              → lưu đánh giá. Field: orderId, rating (1–5), content
// GET  /review/tier                → hạng thành viên + điểm + đánh giá nhận được (tier.jsp)
//      Gửi sang JSP: progress (TierProgress), summary (RatingSummary), reviews (List<ReviewLine>)
@WebServlet(urlPatterns = {"/review/create", "/review/tier"})
public class ReviewServlet extends HttpServlet {

    private final ReviewService reviewService = new ReviewService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String userId = CurrentUser.id(req);
        if (userId == null) {
            CurrentUser.redirectToLogin(req, resp);
            return;
        }
        try {
            if ("/review/tier".equals(req.getServletPath())) {
                req.setAttribute("progress", reviewService.getTierProgress(userId));
                req.setAttribute("summary", reviewService.getSummary(userId));
                req.setAttribute("reviews", reviewService.getReviewsOf(userId));
                req.getRequestDispatcher("/WEB-INF/views/review/tier.jsp").forward(req, resp);
                return;
            }
            String orderId = req.getParameter("orderId");
            Review mine = reviewService.getMyReview(orderId, userId);
            if (mine != null) {                                   // đã đánh giá → mở form sửa, điền sẵn
                req.setAttribute("orderId", orderId);
                req.setAttribute("review", mine);
                req.setAttribute("error", req.getParameter("error"));
                req.getRequestDispatcher("/WEB-INF/views/review/review_form.jsp").forward(req, resp);
                return;
            }
            if (!reviewService.canReview(orderId, userId)) {      // không đủ điều kiện → quay về đơn
                resp.sendRedirect(req.getContextPath() + "/order/detail?id=" + encode(orderId)
                        + "&error=" + encode("Đơn này chưa thể đánh giá hoặc bạn đã đánh giá rồi"));
                return;
            }
            req.setAttribute("orderId", orderId);
            req.setAttribute("error", req.getParameter("error"));
            req.getRequestDispatcher("/WEB-INF/views/review/review_form.jsp").forward(req, resp);
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
        String orderId = req.getParameter("orderId");
        String ctx = req.getContextPath();
        try {
            int rating = Integer.parseInt(req.getParameter("rating"));   // chữ "5" → số 5
            if (reviewService.getMyReview(orderId, userId) != null) {
                reviewService.updateReview(orderId, userId, rating, req.getParameter("content"));
                resp.sendRedirect(ctx + "/order/detail?id=" + encode(orderId) + "&message=" + encode("Đã cập nhật đánh giá"));
                return;
            }
            reviewService.rateUser(orderId, userId, rating, req.getParameter("content"));
            resp.sendRedirect(ctx + "/order/detail?id=" + encode(orderId) + "&message=" + encode("Đã gửi đánh giá"));
        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            // NumberFormatException (không chọn sao) cũng là IllegalArgumentException
            String msg = e instanceof NumberFormatException ? "Vui lòng chọn số sao" : e.getMessage();
            resp.sendRedirect(ctx + "/review/create?orderId=" + encode(orderId) + "&error=" + encode(msg));
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}