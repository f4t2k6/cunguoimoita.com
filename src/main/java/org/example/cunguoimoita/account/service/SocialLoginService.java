package org.example.cunguoimoita.account.service;

import org.example.cunguoimoita.account.dao.UserDAO;
import org.example.cunguoimoita.account.model.User;
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
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Đăng nhập bằng Google/Facebook theo OAuth 2.0 authorization code. */
public class SocialLoginService {

    private static final String FB_VERSION = "v19.0";
    private static final int MAX_NAME_LENGTH = 100;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UnaryOperator<String> config;
    private final UserDAO userDAO = new UserDAO();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public enum Provider {
        GOOGLE("Google", "oauth.google.clientId", "oauth.google.clientSecret"),
        FACEBOOK("Facebook", "oauth.facebook.appId", "oauth.facebook.appSecret");

        private final String label;
        private final String clientIdKey;
        private final String secretKey;

        Provider(String label, String clientIdKey, String secretKey) {
            this.label = label;
            this.clientIdKey = clientIdKey;
            this.secretKey = secretKey;
        }

        public String label() {
            return label;
        }

        public String path() {
            return name().toLowerCase();
        }
    }

    /** Thông tin lấy được từ Google / Facebook. */
    public record Profile(String email, String name) {
    }

    /** @param config đọc tham số cấu hình theo tên, trả "" nếu chưa cấu hình */
    public SocialLoginService(UnaryOperator<String> config) {
        this.config = config;
    }

    public boolean isConfigured(Provider provider) {
        return !config.apply(provider.clientIdKey).isEmpty();
    }

    public static String newState() {
        byte[] buf = new byte[24];
        RANDOM.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    public String authorizationUrl(Provider provider, String redirectUri, String state) {
        Map<String, String> q = new LinkedHashMap<>();
        q.put("client_id", config.apply(provider.clientIdKey));
        q.put("redirect_uri", redirectUri);
        q.put("response_type", "code");
        q.put("state", state);
        if (provider == Provider.GOOGLE) {
            q.put("scope", "openid email profile");
            q.put("prompt", "select_account");
            return "https://accounts.google.com/o/oauth2/v2/auth?" + form(q);
        }
        q.put("scope", "email,public_profile");
        return "https://www.facebook.com/" + FB_VERSION + "/dialog/oauth?" + form(q);
    }

    /** Đổi code lấy access token rồi đọc email và tên. Email Google chưa xác minh trả về null. */
    public Profile fetchProfile(Provider provider, String code, String redirectUri)
            throws IOException, InterruptedException {
        return provider == Provider.GOOGLE ? google(code, redirectUri) : facebook(code, redirectUri);
    }

    /** Email đã có tài khoản thì dùng tài khoản đó; chưa có thì tạo khách hàng mới với mật khẩu ngẫu nhiên. */
    public User findOrCreateCustomer(Profile profile) throws SQLException {
        String email = profile.email().trim().toLowerCase();
        User user = userDAO.findByEmail(email);
        if (user != null) {
            return user;
        }
        String name = profile.name() == null || profile.name().isBlank()
                ? email.substring(0, email.indexOf('@')) : profile.name().trim();
        if (name.length() > MAX_NAME_LENGTH) {
            name = name.substring(0, MAX_NAME_LENGTH);
        }
        byte[] buf = new byte[24];
        RANDOM.nextBytes(buf);
        userDAO.registerCustomer(email, PasswordUtil.hashPassword(Base64.getEncoder().encodeToString(buf)), name, null);
        return userDAO.findByEmail(email);
    }

    private Profile google(String code, String redirectUri) throws IOException, InterruptedException {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("client_id", config.apply(Provider.GOOGLE.clientIdKey));
        body.put("client_secret", config.apply(Provider.GOOGLE.secretKey));
        body.put("redirect_uri", redirectUri);
        body.put("grant_type", "authorization_code");
        String token = requireToken(field(send(HttpRequest.newBuilder(URI.create("https://oauth2.googleapis.com/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form(body)))), "access_token"));
        String info = send(HttpRequest.newBuilder(URI.create("https://openidconnect.googleapis.com/v1/userinfo"))
                .header("Authorization", "Bearer " + token).GET());
        boolean verified = Pattern.compile("\"email_verified\"\\s*:\\s*(true|\"true\")").matcher(info).find();
        return new Profile(verified ? field(info, "email") : null, field(info, "name"));
    }

    private Profile facebook(String code, String redirectUri) throws IOException, InterruptedException {
        Map<String, String> q = new LinkedHashMap<>();
        q.put("client_id", config.apply(Provider.FACEBOOK.clientIdKey));
        q.put("client_secret", config.apply(Provider.FACEBOOK.secretKey));
        q.put("redirect_uri", redirectUri);
        q.put("code", code);
        String token = requireToken(field(send(HttpRequest.newBuilder(URI.create(
                "https://graph.facebook.com/" + FB_VERSION + "/oauth/access_token?" + form(q))).GET()),
                "access_token"));
        Map<String, String> me = new LinkedHashMap<>();
        me.put("fields", "id,name,email");
        me.put("access_token", token);
        String info = send(HttpRequest.newBuilder(URI.create(
                "https://graph.facebook.com/" + FB_VERSION + "/me?" + form(me))).GET());
        return new Profile(field(info, "email"), field(info, "name"));
    }

    private static String requireToken(String token) {
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

    private static String form(Map<String, String> params) {
        return params.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    /** Lấy một trường chuỗi trong JSON phẳng của Google/Facebook, không cần thêm thư viện JSON. */
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
