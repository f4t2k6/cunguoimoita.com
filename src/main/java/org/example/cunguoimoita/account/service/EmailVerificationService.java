package org.example.cunguoimoita.account.service;

import jakarta.mail.MessagingException;
import org.example.cunguoimoita.account.dao.EmailVerificationDAO;
import org.example.cunguoimoita.account.dao.EmailVerificationDAO.Pending;
import org.example.cunguoimoita.account.dao.UserDAO;
import org.example.cunguoimoita.account.model.User;
import org.example.cunguoimoita.common.db.ConnectionPool;
import org.example.cunguoimoita.common.util.MailSender;
import org.example.cunguoimoita.common.util.PasswordUtil;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.logging.Logger;

/** Đăng ký có xác nhận email: gửi mã 6 số, nhập đúng mã mới tạo tài khoản. */
public class EmailVerificationService {
    public static final int VALID_MINUTES = 10;
    public static final int MAX_ATTEMPTS = 5;
    public static final int RESEND_SECONDS = 60;

    private static final Logger LOG = Logger.getLogger(EmailVerificationService.class.getName());
    private static final SecureRandom RANDOM = new SecureRandom();

    private final EmailVerificationDAO dao = new EmailVerificationDAO();
    private final UserDAO userDAO = new UserDAO();
    private final MailSender mail;

    public EmailVerificationService(MailSender mail) {
        this.mail = mail;
    }

    /** Bước 1: lưu thông tin đăng ký và gửi mã về email. */
    public void start(String fullName, String email, String phone, String password)
            throws SQLException {
        if (userDAO.findByEmail(email) != null) {
            throw new VerificationException("Email này đã được sử dụng.");
        }
        String passwordHash = PasswordUtil.hashPassword(password);
        String code = newCode();
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            c.setAutoCommit(false);
            try {
                dao.deleteExpired(c);
                checkCooldown(dao.findForUpdate(c, email));
                dao.upsert(c, email, fullName, phone, passwordHash, hash(email, code), VALID_MINUTES);
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
        sendCode(email, fullName, code);
    }

    /** Gửi lại mã mới cho lượt đăng ký đang chờ. */
    public void resend(String email) throws SQLException {
        String code = newCode();
        String fullName;
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            c.setAutoCommit(false);
            try {
                Pending p = dao.findForUpdate(c, email);
                if (p == null) {
                    throw new VerificationException("Lượt đăng ký đã hết hạn, vui lòng đăng ký lại.");
                }
                checkCooldown(p);
                dao.renewCode(c, email, hash(email, code), VALID_MINUTES);
                c.commit();
                fullName = p.fullname();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
        sendCode(email, fullName, code);
    }

    /** Nhập đúng mã thì tạo tài khoản khách hàng và trả về User để đăng nhập luôn. */
    public User verify(String email, String code) throws SQLException {
        code = code == null ? "" : code.replaceAll("\\s", "");
        if (!code.matches("\\d{6}")) {
            throw new VerificationException("Mã xác nhận gồm 6 chữ số.");
        }
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            c.setAutoCommit(false);
            try {
                Pending p = dao.findForUpdate(c, email);
                if (p == null) {
                    throw new VerificationException("Lượt đăng ký đã hết hạn, vui lòng đăng ký lại.");
                }
                if (p.expiresAt().isBefore(OffsetDateTime.now())) {
                    throw new VerificationException("Mã đã hết hạn. Bấm \"Gửi lại mã\" để nhận mã mới.");
                }
                if (p.attempts() >= MAX_ATTEMPTS) {
                    throw new VerificationException("Bạn đã nhập sai quá " + MAX_ATTEMPTS
                            + " lần. Bấm \"Gửi lại mã\" để nhận mã mới.");
                }
                if (!MessageDigest.isEqual(hash(email, code).getBytes(StandardCharsets.US_ASCII),
                        p.codeHash().getBytes(StandardCharsets.US_ASCII))) {
                    dao.addAttempt(c, email);
                    c.commit();
                    int left = MAX_ATTEMPTS - p.attempts() - 1;
                    throw new VerificationException(left > 0
                            ? "Mã không đúng, bạn còn " + left + " lần thử."
                            : "Mã không đúng. Bấm \"Gửi lại mã\" để nhận mã mới.");
                }
                // Dòng chờ đang bị khoá (FOR UPDATE) nên hai lần bấm cùng lúc không tạo 2 tài khoản
                try {
                    userDAO.registerCustomer(email, p.passwordHash(), p.fullname(), p.phone());
                } catch (SQLException e) {
                    if ("23505".equals(e.getSQLState())) {
                        dao.delete(c, email);
                        c.commit();
                        throw new VerificationException("Email này đã được sử dụng.");
                    }
                    throw e;
                }
                dao.delete(c, email);
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
        return userDAO.findByEmail(email);
    }

    public boolean isMailConfigured() {
        return mail.isConfigured();
    }

    private void checkCooldown(Pending p) {
        if (p == null || p.lastSentAt() == null) {
            return;
        }
        long waited = Duration.between(p.lastSentAt(), OffsetDateTime.now()).getSeconds();
        if (waited >= 0 && waited < RESEND_SECONDS) {
            throw new VerificationException("Mã vừa được gửi, vui lòng chờ "
                    + (RESEND_SECONDS - waited) + " giây rồi thử lại.");
        }
    }

    private void sendCode(String email, String fullName, String code) {
        String text = "Xin chào " + fullName + ",\n\n"
                + "Mã xác nhận đăng ký tài khoản Cunguoimoita của bạn là: " + code + "\n\n"
                + "Mã có hiệu lực trong " + VALID_MINUTES + " phút. Không chia sẻ mã này cho bất kỳ ai.\n"
                + "Nếu bạn không đăng ký tài khoản, hãy bỏ qua email này.\n\n"
                + "Cunguoimoita - Cũ người mới ta";
        try {
            mail.send(email, "Mã xác nhận đăng ký Cunguoimoita: " + code, text);
        } catch (MessagingException e) {
            LOG.warning("Không gửi được email tới " + email + ": " + e);
            throw new VerificationException("Không gửi được email xác nhận. Kiểm tra lại địa chỉ email "
                    + "hoặc thử lại sau ít phút.");
        }
    }

    private static String newCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    /** Không lưu mã gốc, chỉ lưu SHA-256 của email + mã. */
    private static String hash(String email, String code) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256")
                    .digest((email + ":" + code).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
