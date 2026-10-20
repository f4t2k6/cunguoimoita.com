package org.example.cunguoimoita.feed.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.cunguoimoita.common.util.CurrentUser;
import org.example.cunguoimoita.common.util.FormUtil;
import org.example.cunguoimoita.common.util.JsonUtil;
import org.example.cunguoimoita.feed.service.FeedService;

import java.io.IOException;
import java.sql.SQLException;

/** Lưu / bỏ lưu bài. Gọi bằng fetch thì trả JSON, gửi form thường thì redirect về trang cũ. */
@WebServlet("/feed/wishlist")
public class WishlistServlet extends HttpServlet {

    private final FeedService feedService = new FeedService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        boolean ajax = "fetch".equals(req.getHeader("X-Requested-With"));
        String loginUrl = req.getContextPath() + "/login";

        if (CurrentUser.id(req) == null) {
            if (ajax) {
                JsonUtil.write(resp, HttpServletResponse.SC_UNAUTHORIZED,
                        "{\"error\":" + JsonUtil.quote("Đăng nhập để lưu bài") + ",\"login\":" + JsonUtil.quote(loginUrl) + "}");
            } else {
                resp.sendRedirect(loginUrl);
            }
            return;
        }
        if (!FormUtil.isCustomer(req)) {
            fail(req, resp, ajax, HttpServletResponse.SC_FORBIDDEN, "Tài khoản admin không dùng Bucket List");
            return;
        }

        boolean save = !"remove".equals(req.getParameter("action"));
        try {
            feedService.setSaved(CurrentUser.id(req), req.getParameter("productId"), save);
        } catch (IllegalArgumentException e) {
            fail(req, resp, ajax, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            return;
        } catch (SQLException e) {
            throw new ServletException(e);
        }

        if (ajax) {
            JsonUtil.write(resp, HttpServletResponse.SC_OK, "{\"saved\":" + save + "}");
        } else {
            resp.sendRedirect(req.getContextPath() + FormUtil.safeBack(req, "/feed/bucket-list"));
        }
    }

    private void fail(HttpServletRequest req, HttpServletResponse resp, boolean ajax, int status, String message)
            throws IOException {
        if (ajax) {
            JsonUtil.error(resp, status, message);
        } else {
            String back = req.getContextPath() + FormUtil.safeBack(req, "/feed/bucket-list");
            resp.sendRedirect(back + (back.contains("?") ? "&" : "?") + "error=" + FormUtil.encode(message));
        }
    }
}
