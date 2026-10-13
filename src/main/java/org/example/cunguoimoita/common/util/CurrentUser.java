package org.example.cunguoimoita.common.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

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
}
