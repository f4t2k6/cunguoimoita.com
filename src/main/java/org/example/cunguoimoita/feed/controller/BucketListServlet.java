package org.example.cunguoimoita.feed.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.cunguoimoita.common.util.CurrentUser;
import org.example.cunguoimoita.common.util.FormUtil;
import org.example.cunguoimoita.feed.service.FeedService;

import java.io.IOException;
import java.sql.SQLException;

/** Các bài người dùng đã lưu (Bucket List). */
@WebServlet("/feed/bucket-list")
public class BucketListServlet extends HttpServlet {

    private final FeedService feedService = new FeedService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (CurrentUser.id(req) == null) {
            CurrentUser.redirectToLogin(req, resp);
            return;
        }
        if (!FormUtil.isCustomer(req)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Tài khoản admin không có Bucket List");
            return;
        }
        try {
            req.setAttribute("products", feedService.bucketList(CurrentUser.id(req)));
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        req.setAttribute("error", req.getParameter("error"));
        req.getRequestDispatcher("/WEB-INF/views/feed/bucket-list.jsp").forward(req, resp);
    }
}
