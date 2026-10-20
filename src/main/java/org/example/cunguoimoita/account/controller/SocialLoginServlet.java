package org.example.cunguoimoita.account.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.cunguoimoita.account.model.User;
import org.example.cunguoimoita.account.service.SocialLoginService;
import org.example.cunguoimoita.account.service.SocialLoginService.Profile;
import org.example.cunguoimoita.account.service.SocialLoginService.Provider;
import org.example.cunguoimoita.common.model.AccountStatus;
import org.example.cunguoimoita.common.model.UserRole;
import org.example.cunguoimoita.common.util.CurrentUser;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Đăng nhập bằng Google/Facebook: /oauth/{google|facebook} chuyển sang trang của họ, /callback nhận kết quả.
 * Khoá OAuth và app.baseUrl cấu hình bằng &lt;Parameter&gt; trong META-INF/context.xml.
 */
@WebServlet("/oauth/*")
public class SocialLoginServlet extends HttpServlet {
    private static final Logger LOG = Logger.getLogger(SocialLoginServlet.class.getName());
    private static final String STATE_KEY = "oauthState";

    private SocialLoginService socialLogin;

    @Override
    public void init() {
        socialLogin = new SocialLoginService(this::param);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getPathInfo() == null ? "" : req.getPathInfo();
        switch (path) {
            case "/google" -> start(req, resp, Provider.GOOGLE);
            case "/facebook" -> start(req, resp, Provider.FACEBOOK);
            case "/google/callback" -> callback(req, resp, Provider.GOOGLE);
            case "/facebook/callback" -> callback(req, resp, Provider.FACEBOOK);
            default -> resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void start(HttpServletRequest req, HttpServletResponse resp, Provider provider) throws IOException {
        if (!socialLogin.isConfigured(provider)) {
            fail(req, resp, "Đăng nhập bằng " + provider.label() + " chưa được cấu hình trên máy chủ này.");
            return;
        }
        String state = SocialLoginService.newState();
        req.getSession(true).setAttribute(STATE_KEY, provider.path() + ":" + state);
        resp.sendRedirect(socialLogin.authorizationUrl(provider, redirectUri(req, provider), state));
    }

    private void callback(HttpServletRequest req, HttpServletResponse resp, Provider provider)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        Object expected = session == null ? null : session.getAttribute(STATE_KEY);
        if (session != null) {
            session.removeAttribute(STATE_KEY);
        }
        String code = req.getParameter("code");
        // Người dùng bấm Huỷ, hoặc state không khớp (link giả mạo / hết phiên)
        if (code == null || !(provider.path() + ":" + req.getParameter("state")).equals(expected)) {
            fail(req, resp, "Đăng nhập bằng " + provider.label() + " không thành công, vui lòng thử lại.");
            return;
        }
        Profile profile;
        try {
            profile = socialLogin.fetchProfile(provider, code, redirectUri(req, provider));
        } catch (IOException | InterruptedException | IllegalStateException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LOG.warning("Lỗi đăng nhập " + provider.path() + ": " + e);
            fail(req, resp, "Không kết nối được với " + provider.label() + ", vui lòng thử lại.");
            return;
        }
        if (profile.email() == null || profile.email().isBlank()) {
            fail(req, resp, "Tài khoản " + provider.label() + " của bạn không cung cấp email đã xác minh. "
                    + "Hãy đăng ký bằng email.");
            return;
        }
        try {
            User user = socialLogin.findOrCreateCustomer(profile);
            if (user.getStatus() == AccountStatus.BANNED) {
                resp.sendRedirect(req.getContextPath() + "/login?banned=1");
                return;
            }
            String wanted = CurrentUser.logIn(req, user.getId(), user.getRole().databaseValue(),
                    user.getFullName(), user.getAvatarPath());
            CurrentUser.redirectAfterLogin(req, resp, user.getRole() == UserRole.ADMIN, wanted);
        } catch (SQLException e) {
            throw new ServletException("Không thể đăng nhập lúc này.", e);
        }
    }

    private void fail(HttpServletRequest req, HttpServletResponse resp, String message) throws IOException {
        req.getSession(true).setAttribute("loginError", message);
        resp.sendRedirect(req.getContextPath() + "/login");
    }

    /** Địa chỉ Google / Facebook gọi về; phải khai báo y hệt trong trang quản lý ứng dụng của họ. */
    private String redirectUri(HttpServletRequest req, Provider provider) {
        String base = param("app.baseUrl");
        if (base.isEmpty()) {
            boolean defaultPort = ("http".equals(req.getScheme()) && req.getServerPort() == 80)
                    || ("https".equals(req.getScheme()) && req.getServerPort() == 443);
            base = req.getScheme() + "://" + req.getServerName()
                    + (defaultPort ? "" : ":" + req.getServerPort()) + req.getContextPath();
        }
        return base.replaceAll("/+$", "") + "/oauth/" + provider.path() + "/callback";
    }

    // Giá trị mẫu trong context.xml.example có chữ "ĐỔI" nghĩa là chưa cấu hình.
    private String param(String name) {
        String v = getServletContext().getInitParameter(name);
        return v == null || v.isBlank() || v.contains("ĐỔI") ? "" : v.trim();
    }
}
