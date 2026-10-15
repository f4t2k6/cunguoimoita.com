package org.example.cunguoimoita.common.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Lấy id người đang đăng nhập từ session. Không nhận userId từ query string.
 */
public final class CurrentUser {

    public static final String SESSION_KEY = "userId";

    private CurrentUser() {
    }

    /** null nếu chưa đăng nhập. */
    public static String id(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        Object value = session == null ? null : session.getAttribute(SESSION_KEY);
        return value instanceof String ? (String) value : null;
    }

    /** true nếu người đang đăng nhập là admin (LoginServlet lưu userRole vào session). */
    public static boolean isAdmin(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session != null && "admin".equals(session.getAttribute("userRole"));
    }

    /** Chuyển tới trang đăng nhập (dùng cho trang HTML khi chưa đăng nhập). */
    public static void redirectToLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.sendRedirect(req.getContextPath() + "/login");
    }
}
