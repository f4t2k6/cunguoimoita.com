package org.example.cunguoimoita.common.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.cunguoimoita.common.model.UserRole;

import java.io.IOException;

/**
 * Lấy id người đang đăng nhập từ session. Không nhận userId từ query string.
 */
public final class CurrentUser {

    public static final String SESSION_KEY = "userId";
    public static final String ROLE_KEY = "userRole";
    public static final String NAME_KEY = "fullName";
    public static final String AVATAR_KEY = "avatarPath";
    /** Trang khách định mở trước khi bị chuyển sang /login. */
    public static final String REDIRECT_AFTER_LOGIN = "redirectAfterLogin";

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
        return session != null && UserRole.ADMIN.databaseValue().equals(session.getAttribute(ROLE_KEY));
    }

    /** Chuyển tới trang đăng nhập (dùng cho trang HTML khi chưa đăng nhập). */
    public static void redirectToLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.sendRedirect(req.getContextPath() + "/login");
    }

    /**
     * Đổi session id (chống session fixation) rồi lưu người vừa đăng nhập.
     * @return trang khách định mở trước khi phải đăng nhập, null nếu không có
     */
    public static String logIn(HttpServletRequest req, String userId, String role, String fullName, String avatarPath) {
        HttpSession session = req.getSession(true);
        Object wanted = session.getAttribute(REDIRECT_AFTER_LOGIN);
        session.removeAttribute(REDIRECT_AFTER_LOGIN);
        req.changeSessionId();
        session.setAttribute(SESSION_KEY, userId);
        session.setAttribute(ROLE_KEY, role);
        session.setAttribute(NAME_KEY, fullName);
        session.setAttribute(AVATAR_KEY, avatarPath);
        return wanted instanceof String s ? s : null;
    }

    /** Admin về trang quản trị; khách về trang định mở (chỉ đường dẫn nội bộ) hoặc trang chủ. */
    public static void redirectAfterLogin(HttpServletRequest req, HttpServletResponse resp, boolean admin, String wanted)
            throws IOException {
        if (admin) {
            resp.sendRedirect(req.getContextPath() + "/admin/dashboard");
        } else if (wanted != null && wanted.startsWith("/") && !wanted.startsWith("//")) {
            resp.sendRedirect(req.getContextPath() + wanted);
        } else {
            resp.sendRedirect(req.getContextPath() + "/home");
        }
    }
}
