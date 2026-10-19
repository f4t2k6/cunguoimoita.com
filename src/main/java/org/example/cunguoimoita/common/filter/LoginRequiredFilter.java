package org.example.cunguoimoita.common.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.cunguoimoita.common.util.CurrentUser;
import org.example.cunguoimoita.common.util.JsonUtil;

import java.io.IOException;
import java.util.Set;

/**
 * Chưa đăng nhập thì chỉ vào được trang đăng nhập / đăng ký / quên mật khẩu và đăng nhập Google/Facebook (/oauth/*)
 * (khách không xem bảng tin nữa).
 * Mọi trang khác chuyển về /login; fetch/AJAX nhận 401 + JSON {"error", "login"}.
 * Đã đăng nhập mà mở /login hoặc /register thì chuyển về trang chính.
 */
@WebFilter("/*")
public class LoginRequiredFilter implements Filter {

    /** Trang khách được mở. */
    private static final Set<String> GUEST_PAGES = Set.of(
            "/login", "/login.jsp",
            "/register", "/register.jsp",
            "/register/verify", "/verify-email.jsp",
            "/forgot-password");

    /** Đường dẫn công khai cho cả khách lẫn người đã đăng nhập. */
    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/favicon.ico",
            "/payment/vnpay-return"); // VNPay chuyển về, không được chặn kẻo mất kết quả thanh toán

    /** Session lưu trang người dùng định mở trước khi bị chuyển sang đăng nhập. */
    public static final String REDIRECT_AFTER_LOGIN = "redirectAfterLogin";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String path = req.getRequestURI().substring(req.getContextPath().length());

        if (path.startsWith("/assets/") || path.startsWith("/oauth/") || PUBLIC_PATHS.contains(path)) {
            chain.doFilter(request, response);
            return;
        }

        boolean loggedIn = CurrentUser.id(req) != null;
        if (GUEST_PAGES.contains(path)) {
            if (loggedIn && "GET".equals(req.getMethod())) {
                resp.sendRedirect(req.getContextPath()
                        + (CurrentUser.isAdmin(req) ? "/admin/dashboard" : "/home"));
                return;
            }
            chain.doFilter(request, response);
            return;
        }

        if (loggedIn) {
            chain.doFilter(request, response);
            return;
        }

        String loginUrl = req.getContextPath() + "/login";
        if (isAjax(req)) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write("{\"error\":" + JsonUtil.quote("Vui lòng đăng nhập")
                    + ",\"login\":" + JsonUtil.quote(loginUrl) + "}");
            return;
        }
        if ("GET".equals(req.getMethod()) && !path.isEmpty() && !"/".equals(path)
                && !"/index.jsp".equals(path) && !"/home".equals(path)) {
            String query = req.getQueryString();
            req.getSession(true).setAttribute(REDIRECT_AFTER_LOGIN,
                    path + (query == null ? "" : "?" + query));
        }
        resp.sendRedirect(loginUrl);
    }

    private static boolean isAjax(HttpServletRequest req) {
        // Trình duyệt mở trang luôn gửi Accept có text/html; fetch, ảnh, script thì không
        String accept = req.getHeader("Accept");
        return req.getHeader("X-Requested-With") != null
                || accept == null || !accept.contains("text/html");
    }
}
