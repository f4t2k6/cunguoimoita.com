package org.example.cunguoimoita.account.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.cunguoimoita.account.dao.UserDAO;
import org.example.cunguoimoita.account.model.User;
import org.example.cunguoimoita.common.filter.LoginRequiredFilter;
import org.example.cunguoimoita.common.model.AccountStatus;
import org.example.cunguoimoita.common.model.UserRole;
import org.example.cunguoimoita.common.util.PasswordUtil;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Đăng nhập bằng Google / Facebook (OAuth 2.0, luồng authorization code).
 *   /oauth/google, /oauth/facebook            -> chuyển sang trang đăng nhập của Google / Facebook
 *   /oauth/google/callback, /oauth/facebook/callback -> nhận code, đổi lấy email + tên
 * Email đã có tài khoản thì đăng nhập vào tài khoản đó; chưa có thì tạo tài khoản khách hàng mới
 * (mật khẩu ngẫu nhiên, muốn đăng nhập bằng mật khẩu thì dùng "Quên mật khẩu").
 *
 * Cấu hình bằng các dòng <Parameter> trong META-INF/context.xml (xem context.xml.example):
 * oauth.google.clientId, oauth.google.clientSecret, oauth.facebook.appId, oauth.facebook.appSecret,
 * tuỳ chọn app.baseUrl (vd https://cunguoimoita.onrender.com) khi chạy sau proxy.
 */
@WebServlet("/oauth/*")
public class SocialLoginServlet extends HttpServlet {
    private static final Logger LOG = Logger.getLogger(SocialLoginServlet.class.getName());
    private static final String STATE_KEY = "oauthState";
    private static final String FB_VERSION = "v19.0";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserDAO userDAO = new UserDAO();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    /** Thông tin lấy được từ Google / Facebook. */
    private record Profile(String email, String name) {
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getPathInfo() == null ? "" : req.getPathInfo();
        switch (path) {
            case "/google" -> start(req, resp, "google");
            case "/facebook" -> start(req, resp, "facebook");
            case "/google/callback" -> callback(req, resp, "google");
            case "/facebook/callback" -> callback(req, resp, "facebook");
            default -> resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void start(HttpServletRequest req, HttpServletResponse resp, String provider) throws IOException {
        String clientId = param(provider.equals("google") ? "oauth.google.clientId" : "oauth.facebook.appId");
        if (clientId.isEmpty()) {
            fail(req, resp, "Đăng nhập bằng " + label(provider) + " chưa được cấu hình trên máy chủ này.");
            return;
        }
        byte[] buf = new byte[24];
        RANDOM.nextBytes(buf);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
        req.getSession(true).setAttribute(STATE_KEY, provider + ":" + state);

        Map<String, String> q = new LinkedHashMap<>();
        q.put("client_id", clientId);
        q.put("redirect_uri", redirectUri(req, provider));
        q.put("response_type", "code");
        q.put("state", state);
        String url;
        if (provider.equals("google")) {
            q.put("scope", "openid email profile");
            q.put("prompt", "select_account");
            url = "https://accounts.google.com/o/oauth2/v2/auth?" + form(q);
        } else {
            q.put("scope", "email,public_profile");
            url = "https://www.facebook.com/" + FB_VERSION + "/dialog/oauth?" + form(q);
        }
        resp.sendRedirect(url);
    }

    private void callback(HttpServletRequest req, HttpServletResponse resp, String provider)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        Object expected = session == null ? null : session.getAttribute(STATE_KEY);
        if (session != null) {
            session.removeAttribute(STATE_KEY);
        }
        String code = req.getParameter("code");
        if (code == null || !(provider + ":" + req.getParameter("state")).equals(expected)) {
            // Người dùng bấm Huỷ, hoặc state không khớp (link giả mạo / hết phiên)
            fail(req, resp, "Đăng nhập bằng " + label(provider) + " không thành công, vui lòng thử lại.");
            return;
        }
        Profile profile;
        try {
            profile = provider.equals("google") ? google(req, code) : facebook(req, code);
        } catch (IOException | InterruptedException | IllegalStateException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LOG.warning("Lỗi đăng nhập " + provider + ": " + e);
            fail(req, resp, "Không kết nối được với " + label(provider) + ", vui lòng thử lại.");
            return;
        }
        if (profile.email() == null || profile.email().isBlank()) {
            fail(req, resp, "Tài khoản " + label(provider) + " của bạn không cung cấp email đã xác minh. "
                    + "Hãy đăng ký bằng email.");
            return;
        }
        try {
            String email = profile.email().trim().toLowerCase();
            User user = userDAO.findByEmail(email);
            if (user == null) {
                String name = profile.name() == null || profile.name().isBlank()
                        ? email.substring(0, email.indexOf('@')) : profile.name().trim();
                if (name.length() > 100) {
                    name = name.substring(0, 100);
                }
                byte[] buf = new byte[24];
                RANDOM.nextBytes(buf);
                userDAO.registerCustomer(email,
                        PasswordUtil.hashPassword(Base64.getEncoder().encodeToString(buf)), name, null);
                user = userDAO.findByEmail(email);
            }
            if (user.getStatus() == AccountStatus.BANNED) {
                resp.sendRedirect(req.getContextPath() + "/login?banned=1");
                return;
            }
            logIn(req, resp, user);
        } catch (SQLException e) {
            throw new ServletException("Không thể đăng nhập lúc này.", e);
        }
    }

    private Profile google(HttpServletRequest req, String code) throws IOException, InterruptedException {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("client_id", param("oauth.google.clientId"));
        body.put("client_secret", param("oauth.google.clientSecret"));
        body.put("redirect_uri", redirectUri(req, "google"));
        body.put("grant_type", "authorization_code");
        String token = require(field(send(HttpRequest.newBuilder(URI.create("https://oauth2.googleapis.com/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form(body)))), "access_token"));
        String info = send(HttpRequest.newBuilder(URI.create("https://openidconnect.googleapis.com/v1/userinfo"))
                .header("Authorization", "Bearer " + token).GET());
        // Chỉ nhận email Google đã xác minh
        boolean verified = Pattern.compile("\"email_verified\"\\s*:\\s*(true|\"true\")").matcher(info).find();
        return new Profile(verified ? field(info, "email") : null, field(info, "name"));
    }

    private Profile facebook(HttpServletRequest req, String code) throws IOException, InterruptedException {
        Map<String, String> q = new LinkedHashMap<>();
        q.put("client_id", param("oauth.facebook.appId"));
        q.put("client_secret", param("oauth.facebook.appSecret"));
        q.put("redirect_uri", redirectUri(req, "facebook"));
        q.put("code", code);
        String token = require(field(send(HttpRequest.newBuilder(URI.create(
                "https://graph.facebook.com/" + FB_VERSION + "/oauth/access_token?" + form(q))).GET()),
                "access_token"));
        Map<String, String> me = new LinkedHashMap<>();
        me.put("fields", "id,name,email");
        me.put("access_token", token);
        String info = send(HttpRequest.newBuilder(URI.create(
                "https://graph.facebook.com/" + FB_VERSION + "/me?" + form(me))).GET());
        return new Profile(field(info, "email"), field(info, "name"));
    }

    private static String require(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Không nhận được access_token");
        }
        return token;
    }

    private String send(HttpRequest.Builder builder) throws IOException, InterruptedException {
        HttpResponse<String> r = http.send(builder.timeout(Duration.ofSeconds(15)).build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (r.statusCode() / 100 != 2) {
            throw new IllegalStateException("HTTP " + r.statusCode() + ": " + r.body());
        }
        return r.body();
    }

    /** Giống LoginServlet: đổi session id, lưu thông tin người dùng, về trang định mở hoặc trang chủ. */
    private void logIn(HttpServletRequest req, HttpServletResponse resp, User user) throws IOException {
        HttpSession session = req.getSession(true);
        Object wanted = session.getAttribute(LoginRequiredFilter.REDIRECT_AFTER_LOGIN);
        session.removeAttribute(LoginRequiredFilter.REDIRECT_AFTER_LOGIN);
        req.changeSessionId();
        session.setAttribute("userId", user.getId());
        session.setAttribute("userRole", user.getRole().databaseValue());
        session.setAttribute("fullName", user.getFullName());
        session.setAttribute("avatarPath", user.getAvatarPath());
        if (user.getRole() == UserRole.ADMIN) {
            resp.sendRedirect(req.getContextPath() + "/admin/dashboard");
        } else if (wanted instanceof String && ((String) wanted).startsWith("/")
                && !((String) wanted).startsWith("//")) {
            resp.sendRedirect(req.getContextPath() + wanted);
        } else {
            resp.sendRedirect(req.getContextPath() + "/home");
        }
    }

    private void fail(HttpServletRequest req, HttpServletResponse resp, String message) throws IOException {
        req.getSession(true).setAttribute("loginError", message);
        resp.sendRedirect(req.getContextPath() + "/login");
    }

    /** Địa chỉ Google / Facebook gọi về; phải khai báo y hệt trong trang quản lý ứng dụng của họ. */
    private String redirectUri(HttpServletRequest req, String provider) {
        String base = param("app.baseUrl");
        if (base.isEmpty()) {
            boolean defaultPort = ("http".equals(req.getScheme()) && req.getServerPort() == 80)
                    || ("https".equals(req.getScheme()) && req.getServerPort() == 443);
            base = req.getScheme() + "://" + req.getServerName()
                    + (defaultPort ? "" : ":" + req.getServerPort()) + req.getContextPath();
        }
        return base.replaceAll("/+$", "") + "/oauth/" + provider + "/callback";
    }

    private String param(String name) {
        String v = getServletContext().getInitParameter(name);
        return v == null || v.isBlank() || v.contains("ĐỔI") ? "" : v.trim();
    }

    private static String label(String provider) {
        return provider.equals("google") ? "Google" : "Facebook";
    }

    private static String form(Map<String, String> params) {
        return params.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    /** Lấy một trường chuỗi ở JSON trả về (đủ dùng cho JSON phẳng của Google/Facebook, không cần thêm thư viện). */
    private static String field(String json, String name) {
        Matcher m = Pattern.compile("\"" + Pattern.quote(name) + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"")
                .matcher(json);
        if (!m.find()) {
            return null;
        }
        String s = m.group(1);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch != '\\' || i + 1 >= s.length()) {
                out.append(ch);
                continue;
            }
            char n = s.charAt(++i);
            switch (n) {
                case 'u' -> {
                    out.append((char) Integer.parseInt(s.substring(i + 1, i + 5), 16));
                    i += 4;
                }
                case 'n' -> out.append('\n');
                case 't' -> out.append('\t');
                case 'r' -> out.append('\r');
                case 'b' -> out.append('\b');
                case 'f' -> out.append('\f');
                default -> out.append(n);
            }
        }
        return out.toString();
    }
}
