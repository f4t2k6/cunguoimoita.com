package org.example.cunguoimoita.account.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.cunguoimoita.account.dao.UserDAO;
import org.example.cunguoimoita.account.model.User;
import org.example.cunguoimoita.account.service.OAuthJson;
import org.example.cunguoimoita.account.service.SocialLoginException;
import org.example.cunguoimoita.common.model.AccountStatus;
import org.example.cunguoimoita.common.model.UserRole;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/** OAuth authorization-code flow cho Facebook và Google; không dùng framework đăng nhập. */
@WebServlet("/oauth/*")
public class SocialOAuthServlet extends HttpServlet {
    private static final long STATE_LIFETIME_MILLIS = Duration.ofMinutes(10).toMillis();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();
    private static final Logger LOG = Logger.getLogger(SocialOAuthServlet.class.getName());
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getPathInfo();
        String[] parts = path == null ? new String[0] : path.split("/");
        if (parts.length != 3) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String provider = parts[1].toLowerCase(Locale.ROOT);
        String action = parts[2].toLowerCase(Locale.ROOT);
        if (!(provider.equals("google") || provider.equals("facebook"))) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (action.equals("start")) {
            start(req, resp, provider);
        } else if (action.equals("callback")) {
            callback(req, resp, provider);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void start(HttpServletRequest req, HttpServletResponse resp, String provider) throws IOException {
        ProviderConfig config = ProviderConfig.fromEnvironment(provider);
        if (!config.configured()) {
            redirectError(req, resp, provider, "not_configured");
            return;
        }
        String state = newState();
        HttpSession session = req.getSession(true);
        session.setAttribute(stateKey(provider), state);
        session.setAttribute(timeKey(provider), System.currentTimeMillis());
        String from = "register".equals(req.getParameter("from")) ? "register" : "login";
        session.setAttribute(fromKey(provider), from);
        resp.sendRedirect(authorizationUrl(provider, config, state));
    }

    private void callback(HttpServletRequest req, HttpServletResponse resp, String provider)
            throws IOException, ServletException {
        HttpSession session = req.getSession(false);
        String expected = session == null ? null : (String) session.getAttribute(stateKey(provider));
        Object started = session == null ? null : session.getAttribute(timeKey(provider));
        String from = session == null ? "login" : String.valueOf(session.getAttribute(fromKey(provider)));
        if (session != null) {
            session.removeAttribute(stateKey(provider));
            session.removeAttribute(timeKey(provider));
            session.removeAttribute(fromKey(provider));
        }
        String returned = req.getParameter("state");
        boolean fresh = started instanceof Long time
                && System.currentTimeMillis() - time <= STATE_LIFETIME_MILLIS;
        if (expected == null || returned == null || !fresh
                || !MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                returned.getBytes(StandardCharsets.UTF_8))) {
            redirectError(req, resp, from, "oauth_failed");
            return;
        }
        if (req.getParameter("error") != null) {
            redirectError(req, resp, from, "oauth_cancelled");
            return;
        }

        String code = req.getParameter("code");
        if (code == null || code.isBlank()) {
            redirectError(req, resp, from, "oauth_failed");
            return;
        }
        try {
            ProviderConfig config = ProviderConfig.fromEnvironment(provider);
            if (!config.configured()) {
                redirectError(req, resp, from, "not_configured");
                return;
            }
            Profile profile = loadProfile(provider, config, code);
            User user = userDAO.findOrCreateSocialCustomer(provider, profile.id(), profile.email(),
                    profile.emailVerified(), profile.name(), profile.avatarUrl());
            if (user.getStatus() == AccountStatus.BANNED) {
                redirectError(req, resp, from, "banned");
                return;
            }
            if (user.getRole() != UserRole.CUSTOMER) {
                redirectError(req, resp, from, "oauth_failed");
                return;
            }
            HttpSession authenticated = req.getSession(true);
            req.changeSessionId();
            authenticated.setAttribute("userId", user.getId());
            authenticated.setAttribute("userRole", user.getRole().databaseValue());
            authenticated.setAttribute("fullName", user.getFullName());
            authenticated.setAttribute("avatarPath", user.getAvatarPath());
            resp.sendRedirect(req.getContextPath() + "/home");
        } catch (SocialLoginException e) {
            redirectError(req, resp, from, messageCode(e));
        } catch (SQLException e) {
            throw new ServletException("Không thể lưu tài khoản mạng xã hội.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServletException("Đăng nhập mạng xã hội bị gián đoạn.", e);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "OAuth provider request failed: " + provider, e);
            redirectError(req, resp, from, "oauth_failed");
        }
    }

    private Profile loadProfile(String provider, ProviderConfig config, String code)
            throws IOException, InterruptedException, SocialLoginException {
        String tokenEndpoint;
        String form;
        if (provider.equals("google")) {
            tokenEndpoint = "https://oauth2.googleapis.com/token";
            form = form("code", code) + "&" + form("client_id", config.clientId())
                    + "&" + form("client_secret", config.clientSecret())
                    + "&" + form("redirect_uri", config.redirectUri())
                    + "&grant_type=authorization_code";
        } else {
            tokenEndpoint = graphBase() + "/oauth/access_token";
            form = form("code", code) + "&" + form("client_id", config.clientId())
                    + "&" + form("client_secret", config.clientSecret())
                    + "&" + form("redirect_uri", config.redirectUri());
        }
        String tokenJson = postJson(tokenEndpoint, form);
        String accessToken = OAuthJson.string(tokenJson, "access_token");
        if (accessToken == null || accessToken.isBlank()) {
            throw new SocialLoginException("Nhà cung cấp không trả về access token.");
        }

        String profileEndpoint = provider.equals("google")
                ? "https://openidconnect.googleapis.com/v1/userinfo"
                : graphBase() + "/me?fields=id,name,email,picture.type(large)";
        String profileJson = getJson(profileEndpoint, accessToken);
        String id = OAuthJson.string(profileJson, provider.equals("google") ? "sub" : "id");
        String email = OAuthJson.string(profileJson, "email");
        String name = OAuthJson.string(profileJson, "name");
        String avatar = provider.equals("google") ? OAuthJson.string(profileJson, "picture") : null;
        boolean verified = provider.equals("google") && OAuthJson.bool(profileJson, "email_verified");
        if (id == null || id.isBlank() || email == null || email.isBlank()) {
            throw new SocialLoginException("Tài khoản mạng xã hội chưa cung cấp email. Hãy đăng ký bằng email.");
        }
        if (provider.equals("google") && !verified) {
            throw new SocialLoginException("Google chưa xác nhận địa chỉ email này.");
        }
        return new Profile(id, email, name, avatar, verified);
    }

    private String authorizationUrl(String provider, ProviderConfig config, String state) {
        if (provider.equals("google")) {
            return "https://accounts.google.com/o/oauth2/v2/auth?"
                    + form("client_id", config.clientId())
                    + "&" + form("redirect_uri", config.redirectUri())
                    + "&response_type=code&scope=" + encode("openid email profile")
                    + "&prompt=select_account&state=" + encode(state);
        }
        return "https://www.facebook.com/" + graphVersion() + "/dialog/oauth?"
                + form("client_id", config.clientId())
                + "&" + form("redirect_uri", config.redirectUri())
                + "&response_type=code&scope=" + encode("email,public_profile")
                + "&state=" + encode(state);
    }

    private String postJson(String url, String form) throws IOException, InterruptedException,
            SocialLoginException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(form)).build();
        return sendJson(request);
    }

    private String getJson(String url, String accessToken) throws IOException, InterruptedException,
            SocialLoginException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .GET().build();
        return sendJson(request);
    }

    private String sendJson(HttpRequest request) throws IOException, InterruptedException,
            SocialLoginException {
        HttpResponse<String> response = HTTP.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            // Không ghi response body vì lỗi OAuth có thể chứa dữ liệu nhạy cảm.
            throw new SocialLoginException("Không xác thực được với nhà cung cấp.");
        }
        return response.body();
    }

    private void redirectError(HttpServletRequest req, HttpServletResponse resp, String from, String code)
            throws IOException {
        String target = "register".equals(from) ? "/register.jsp" : "/login";
        resp.sendRedirect(req.getContextPath() + target + "?socialError=" + encode(code));
    }

    private String messageCode(SocialLoginException e) {
        if (e.getMessage() != null && e.getMessage().contains("đang bị khóa")) return "banned";
        if (e.getMessage() != null && e.getMessage().contains("Email đã có tài khoản")) return "email_exists";
        return "oauth_failed";
    }

    private String graphBase() {
        return "https://graph.facebook.com/" + graphVersion();
    }

    private String graphVersion() {
        String configured = env("FACEBOOK_GRAPH_VERSION");
        return configured.matches("v\\d+\\.\\d+") ? configured : "v26.0";
    }

    private static String newState() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String form(String name, String value) {
        return encode(name) + "=" + encode(value);
    }

    private static String env(String name) {
        String value = System.getenv(name);
        return value == null ? "" : value.trim();
    }

    private static String stateKey(String provider) { return "social.oauth.state." + provider; }
    private static String timeKey(String provider) { return "social.oauth.time." + provider; }
    private static String fromKey(String provider) { return "social.oauth.from." + provider; }

    private record ProviderConfig(String clientId, String clientSecret, String redirectUri) {
        static ProviderConfig fromEnvironment(String provider) {
            String prefix = provider.equals("google") ? "GOOGLE" : "FACEBOOK";
            return new ProviderConfig(env(prefix + (provider.equals("google") ? "_CLIENT_ID" : "_APP_ID")),
                    env(prefix + (provider.equals("google") ? "_CLIENT_SECRET" : "_APP_SECRET")),
                    env(prefix + "_REDIRECT_URI"));
        }

        boolean configured() {
            return !clientId.isBlank() && !clientSecret.isBlank() && !redirectUri.isBlank()
                    && (redirectUri.startsWith("http://localhost:") || redirectUri.startsWith("https://"));
        }
    }

    private record Profile(String id, String email, String name, String avatarUrl, boolean emailVerified) { }
}
