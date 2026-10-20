package org.example.cunguoimoita.common.util;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.ServletContext;

import java.io.UnsupportedEncodingException;
import java.util.Properties;
import java.util.logging.Logger;

/** Gửi email qua SMTP (mặc định Gmail). Chưa cấu hình mail.username/mail.password thì chỉ in nội dung ra log. */
public final class MailSender {
    private static final Logger LOG = Logger.getLogger(MailSender.class.getName());

    private final String username;
    private final String password;
    private final String from;
    private final Session session;

    public MailSender(ServletContext ctx) {
        this.username = param(ctx, "mail.username", "");
        this.password = param(ctx, "mail.password", "").replace(" ", "");   // App Password Gmail hay được chép kèm dấu cách
        this.from = param(ctx, "mail.from", username);
        Properties props = new Properties();
        props.put("mail.smtp.host", param(ctx, "mail.host", "smtp.gmail.com"));
        props.put("mail.smtp.port", param(ctx, "mail.port", "587"));
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");
        this.session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
    }

    /** true nếu đã có tài khoản gửi mail; false là chế độ thử (in ra log). */
    public boolean isConfigured() {
        return !username.isBlank() && !password.isBlank() && !username.contains("ĐỔI");
    }

    public void send(String to, String subject, String text) throws MessagingException {
        if (!isConfigured()) {
            LOG.warning("[CHƯA CẤU HÌNH EMAIL] Thư gửi " + to + " | " + subject + "\n" + text);
            return;
        }
        MimeMessage msg = new MimeMessage(session);
        try {
            msg.setFrom(new InternetAddress(from, "Cunguoimoita", "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            msg.setFrom(new InternetAddress(from));
        }
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        msg.setSubject(subject, "UTF-8");
        msg.setText(text, "UTF-8");
        Transport.send(msg);
    }

    private static String param(ServletContext ctx, String name, String fallback) {
        String v = ctx.getInitParameter(name);
        return v == null || v.isBlank() ? fallback : v.trim();
    }
}
