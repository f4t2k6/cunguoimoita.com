package org.example.cunguoimoita.account.service;

import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpSession;
import org.example.cunguoimoita.account.dao.UserDAO;
import org.example.cunguoimoita.account.model.User;
import org.example.cunguoimoita.common.model.AccountStatus;
import org.example.cunguoimoita.common.util.MailSender;
import org.example.cunguoimoita.common.util.PasswordUtil;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Quên mật khẩu: gửi mã 6 số, nhập mã, đặt mật khẩu mới.
 * Mã chỉ sống 10 phút nên lưu (đã băm SHA-256) trong session, không cần thêm bảng.
 */
public class PasswordResetService {
    public static final int VALID_MINUTES = EmailVerificationService.VALID_MINUTES;
    public static final int MAX_ATTEMPTS = EmailVerificationService.MAX_ATTEMPTS;
    public static final int RESEND_SECONDS = EmailVerificationService.RESEND_SECONDS;

    private static final String KEY = "passwordReset";
    private static final Logger LOG = Logger.getLogger(PasswordResetService.class.getName());
    private static final SecureRandom RANDOM = new SecureRandom();
    /** Lần gửi mã gần nhất theo email, chặn gửi dồn dập kể cả khi đổi session. */
    private static final Map<String, Long> LAST_SENT = new ConcurrentHashMap<>();

    private final UserDAO userDAO = new UserDAO();
    private final MailSender mail;

    /** Trạng thái đặt lại mật khẩu của một session. */
    private static final class State {
        String email;
        String codeHash;
        long expiresAt;
        int attempts;
        boolean verified;
    }

    public PasswordResetService(MailSender mail) {
        this.mail = mail;
    }

    /** Bước 1: kiểm tra email có tài khoản rồi gửi mã. */
    public void sendCode(HttpSession session, String rawEmail) throws SQLException {
        String email = rawEmail == null ? "" : rawEmail.trim().toLowerCase(Locale.ROOT);
        if (!email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new VerificationException("Vui lòng nhập đúng địa chỉ email.");
        }
        User user = userDAO.findByEmail(email);
        if (user == null) {
            throw new VerificationException("Không tìm thấy tài khoản nào dùng email này.");
        }
        if (user.getStatus() == AccountStatus.BANNED) {
            throw new VerificationException("Tài khoản này đang bị khóa.");
        }
        long now = System.currentTimeMillis();
        Long last = LAST_SENT.get(email);
        if (last != null && now - last < RESEND_SECONDS * 1000L) {
            throw new VerificationException("Mã vừa được gửi, vui lòng chờ "
                    + (RESEND_SECONDS - (now - last) / 1000) + " giây rồi thử lại.");
        }
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        String text = "Xin chào " + user.getFullName() + ",\n\n"
                + "Mã đặt lại mật khẩu Cunguoimoita của bạn là: " + code + "\n\n"
                + "Mã có hiệu lực trong " + VALID_MINUTES + " phút. Không chia sẻ mã này cho bất kỳ ai.\n"
                + "Nếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này.\n\n"
                + "Cunguoimoita - Cũ người mới ta";
        try {
            mail.send(email, "Mã đặt lại mật khẩu Cunguoimoita: " + code, text);
        } catch (MessagingException e) {
            LOG.warning("Không gửi được email tới " + email + ": " + e);
            throw new VerificationException("Không gửi được email. Vui lòng thử lại sau ít phút.");
        }
        LAST_SENT.put(email, now);
        State s = new State();
        s.email = email;
        s.codeHash = hash(email, code);
        s.expiresAt = now + VALID_MINUTES * 60_000L;
        session.setAttribute(KEY, s);
    }

    /** Gửi lại mã cho email đang đặt lại. */
    public void resend(HttpSession session) throws SQLException {
        sendCode(session, current(session).email);
    }

    /** Bước 2: kiểm tra mã. */
    public void verifyCode(HttpSession session, String rawCode) {
        State s = current(session);
        String code = rawCode == null ? "" : rawCode.replaceAll("\\s", "");
        if (!code.matches("\\d{6}")) {
            throw new VerificationException("Mã xác nhận gồm 6 chữ số.");
        }
        if (System.currentTimeMillis() > s.expiresAt) {
            throw new VerificationException("Mã đã hết hạn. Bấm \"Gửi lại mã\" để nhận mã mới.");
        }
        if (s.attempts >= MAX_ATTEMPTS) {
            throw new VerificationException("Bạn đã nhập sai quá " + MAX_ATTEMPTS
                    + " lần. Bấm \"Gửi lại mã\" để nhận mã mới.");
        }
        if (!MessageDigest.isEqual(hash(s.email, code).getBytes(StandardCharsets.US_ASCII),
                s.codeHash.getBytes(StandardCharsets.US_ASCII))) {
            s.attempts++;
            int left = MAX_ATTEMPTS - s.attempts;
            throw new VerificationException(left > 0
                    ? "Mã không đúng, bạn còn " + left + " lần thử."
                    : "Mã không đúng. Bấm \"Gửi lại mã\" để nhận mã mới.");
        }
        s.verified = true;
    }

    /** Bước 3: đặt mật khẩu mới. Trả về email để trang đăng nhập điền sẵn. */
    public String resetPassword(HttpSession session, String password, String confirm) throws SQLException {
        State s = current(session);
        if (!s.verified || System.currentTimeMillis() > s.expiresAt) {
            throw new VerificationException("Phiên đặt lại mật khẩu đã hết hạn, vui lòng làm lại từ đầu.");
        }
        String weak = PasswordUtil.checkStrength(password);
        if (weak != null) {
            throw new VerificationException(weak);
        }
        if (!password.equals(confirm)) {
            throw new VerificationException("Mật khẩu nhập lại chưa khớp.");
        }
        User user = userDAO.findByEmail(s.email);
        if (user == null || !userDAO.updatePasswordHash(user.getId(), PasswordUtil.hashPassword(password))) {
            throw new VerificationException("Không đặt lại được mật khẩu cho tài khoản này.");
        }
        session.removeAttribute(KEY);
        LoginAttemptLimiter.getInstance().recordSuccess(s.email);   // mở khoá nếu trước đó nhập sai nhiều lần
        return s.email;
    }

    public boolean isMailConfigured() {
        return mail.isConfigured();
    }

    private static State current(HttpSession session) {
        Object s = session == null ? null : session.getAttribute(KEY);
        if (!(s instanceof State)) {
            throw new VerificationException("Phiên đặt lại mật khẩu đã hết hạn, vui lòng nhập lại email.");
        }
        return (State) s;
    }

    /** Không giữ mã gốc, chỉ giữ SHA-256 của email + mã. */
    private static String hash(String email, String code) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256")
                    .digest(("reset:" + email + ":" + code).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
