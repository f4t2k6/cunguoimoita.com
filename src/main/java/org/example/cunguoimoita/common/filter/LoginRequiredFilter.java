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
 * Chế độ khách: chưa đăng nhập vẫn xem được bảng tin (/home), tìm kiếm, chi tiết bài đăng và ảnh.
 * Mọi thao tác khác (lưu bài, giỏ hàng, mua, nhắn tin, trả giá, đăng bài, hồ sơ, đơn hàng...) chuyển về /login;
 * fetch/AJAX nhận 401 + JSON {"error", "login"}.
 * Trang đang xem được nhớ lại (redirectAfterLogin) để đăng nhập xong quay về đúng chỗ.
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

    /** Trang khách xem được (chỉ GET): trang chủ, bảng tin, tìm kiếm, chi tiết bài đăng. */
    private static final Set<String> GUEST_BROWSE = Set.of(
            "", "/", "/index.jsp", "/home", "/feed/search", "/product");

    /** Ảnh, CSS, JS: ai cũng tải được để khách xem bảng tin có đủ ảnh. */
    private static final String[] PUBLIC_PREFIXES = {
            "/assets/", "/oauth/", "/media/", "/uploads/", "/product-image/", "/avatar/"};

    /** Session lưu trang người dùng định mở trước khi bị chuyển sang đăng nhập. */
    public static final String REDIRECT_AFTER_LOGIN = "redirectAfterLogin";

    /** Request attribute: trang khách đang xem (path + query), header.jsp gắn vào link Đăng nhập / Đăng ký. */
    public static final String LOGIN_NEXT = "loginNext";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        // Đường dẫn đã giải mã, không kèm ";jsessionid=..." (getRequestURI có thể dính phần này ở lần mở đầu tiên)
        String path = req.getServletPath() + (req.getPathInfo() == null ? "" : req.getPathInfo());

        if (isPublic(path)) {
            chain.doFilter(request, response);
            return;
        }

        boolean loggedIn = CurrentUser.id(req) != null;
        boolean isGet = "GET".equals(req.getMethod());
        if (GUEST_PAGES.contains(path)) {
            if (loggedIn && isGet) {
                resp.sendRedirect(req.getContextPath()
                        + (CurrentUser.isAdmin(req) ? "/admin/dashboard" : "/home"));
                return;
            }
            // Link Đăng nhập / Đăng ký mang ?next=trang đang xem → đăng nhập xong quay về trang đó
            String next = req.getParameter("next");
            if (isGet && isSafeLocal(next)) {
                req.getSession(true).setAttribute(REDIRECT_AFTER_LOGIN, next);
            }
            chain.doFilter(request, response);
            return;
        }

        if (loggedIn) {
            chain.doFilter(request, response);
            return;
        }

        // Khách: xem bảng tin, tìm kiếm, chi tiết bài
        if (isGet && GUEST_BROWSE.contains(path)) {
            String query = req.getQueryString();
            req.setAttribute(LOGIN_NEXT, ("".equals(path) || "/index.jsp".equals(path) ? "/home" : path)
                    + (query == null ? "" : "?" + query));
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
        String wanted = null;
        if (isGet) {
            String query = req.getQueryString();
            wanted = path + (query == null ? "" : "?" + query);
        } else {
            // Bấm nút (POST) khi chưa đăng nhập: đăng nhập xong quay về trang có nút đó
            wanted = refererPath(req);
        }
        if (isSafeLocal(wanted)) {
            req.getSession(true).setAttribute(REDIRECT_AFTER_LOGIN, wanted);
        }
        resp.sendRedirect(loginUrl);
    }

    private static boolean isPublic(String path) {
        if (PUBLIC_PATHS.contains(path)) {
            return true;
        }
        for (String prefix : PUBLIC_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /** Chỉ nhận đường dẫn trong web này (bắt đầu bằng "/", không phải "//host"). */
    private static boolean isSafeLocal(String path) {
        return path != null && path.startsWith("/") && !path.startsWith("//") && !path.contains("\\")
                && !path.startsWith("/login") && !path.startsWith("/register") && !path.startsWith("/logout");
    }

    /** Trang trước đó (header Referer) nếu cùng web này, bỏ phần host và context path. */
    private static String refererPath(HttpServletRequest req) {
        String referer = req.getHeader("Referer");
        if (referer == null) {
            return null;
        }
        try {
            java.net.URI uri = java.net.URI.create(referer);
            if (uri.getHost() != null && !uri.getHost().equalsIgnoreCase(req.getServerName())) {
                return null;
            }
            String p = uri.getRawPath();
            String ctx = req.getContextPath();
            if (p == null || !p.startsWith(ctx)) {
                return null;
            }
            p = p.substring(ctx.length());
            return p + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static boolean isAjax(HttpServletRequest req) {
        // Trình duyệt mở trang luôn gửi Accept có text/html; fetch, ảnh, script thì không
        String accept = req.getHeader("Accept");
        return req.getHeader("X-Requested-With") != null
                || accept == null || !accept.contains("text/html");
    }
}
