package org.example.cunguoimoita.review.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.cunguoimoita.common.util.CurrentUser;
import org.example.cunguoimoita.common.util.FormUtil;
import org.example.cunguoimoita.review.model.Review;
import org.example.cunguoimoita.review.service.ReviewService;

import java.io.IOException;
import java.sql.SQLException;

// Đánh giá sau khi đơn hoàn tất và trang hạng thành viên.
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
            if (mine != null) {
                req.setAttribute("orderId", orderId);
                req.setAttribute("review", mine);
                req.setAttribute("error", req.getParameter("error"));
                req.getRequestDispatcher("/WEB-INF/views/review/review_form.jsp").forward(req, resp);
                return;
            }
            if (!reviewService.canReview(orderId, userId)) {
                resp.sendRedirect(req.getContextPath() + "/order/detail?id=" + FormUtil.encode(orderId)
                        + "&error=" + FormUtil.encode("Đơn này chưa thể đánh giá hoặc bạn đã đánh giá rồi"));
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
            int rating = Integer.parseInt(req.getParameter("rating"));
            if (reviewService.getMyReview(orderId, userId) != null) {
                reviewService.updateReview(orderId, userId, rating, req.getParameter("content"));
                resp.sendRedirect(ctx + "/order/detail?id=" + FormUtil.encode(orderId) + "&message=" + FormUtil.encode("Đã cập nhật đánh giá"));
                return;
            }
            reviewService.rateUser(orderId, userId, rating, req.getParameter("content"));
            resp.sendRedirect(ctx + "/order/detail?id=" + FormUtil.encode(orderId) + "&message=" + FormUtil.encode("Đã gửi đánh giá"));
        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            // NumberFormatException (không chọn sao) cũng là IllegalArgumentException
            String msg = e instanceof NumberFormatException ? "Vui lòng chọn số sao" : e.getMessage();
            resp.sendRedirect(ctx + "/review/create?orderId=" + FormUtil.encode(orderId) + "&error=" + FormUtil.encode(msg));
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }
}