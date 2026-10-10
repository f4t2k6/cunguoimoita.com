package org.example.cunguoimoita.common.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Lấy id người đang đăng nhập từ session (attribute "userId").
 * TẠM THỜI khi F1 (TV1) chưa có đăng nhập: thêm ?as=<userId> vào URL bất kỳ để giả làm user đó.
 * Khi TV1 làm xong login thì bỏ phần "as" và để AuthFilter đặt "userId" vào session.
 */
public final class CurrentUser {

    public static final String SESSION_KEY = "userId";

    private CurrentUser() {
    }

    /** null nếu chưa đăng nhập. */
    public static String id(HttpServletRequest req) {
        String as = req.getParameter("as");
        if (as != null && !as.isBlank()) {
            req.getSession().setAttribute(SESSION_KEY, as.trim());
        }
        HttpSession session = req.getSession(false);
        return session == null ? null : (String) session.getAttribute(SESSION_KEY);
    }
}
