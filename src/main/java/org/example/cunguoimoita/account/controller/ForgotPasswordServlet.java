package org.example.cunguoimoita.account.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.cunguoimoita.account.service.PasswordResetService;
import org.example.cunguoimoita.account.service.VerificationException;
import org.example.cunguoimoita.common.util.JsonUtil;
import org.example.cunguoimoita.common.util.MailSender;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Quên mật khẩu ngay trong khung đăng nhập (assets/js/login.js gọi bằng fetch).
 * POST action=send (email) | resend | verify (code) | reset (password, confirmPassword).
 * Trả JSON {"ok":true, ...} hoặc {"ok":false,"error":"..."}.
 */
@WebServlet("/forgot-password")
public class ForgotPasswordServlet extends HttpServlet {
    private PasswordResetService reset;

    @Override
    public void init() {
        reset = new PasswordResetService(new MailSender(getServletContext()));
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.sendRedirect(req.getContextPath() + "/login");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        HttpSession session = req.getSession(true);
        String action = req.getParameter("action");
        try {
            if ("send".equals(action) || "resend".equals(action)) {
                if ("send".equals(action)) {
                    reset.sendCode(session, req.getParameter("email"));
                } else {
                    reset.resend(session);
                }
                String note = reset.isMailConfigured()
                        ? "Đã gửi mã 6 số tới email của bạn (xem cả mục Spam). Mã có hiệu lực "
                        + PasswordResetService.VALID_MINUTES + " phút."
                        : "Chưa cấu hình gửi email: mã đang được in ra log của Tomcat.";
                JsonUtil.write(resp, 200, "{\"ok\":true,\"message\":" + JsonUtil.quote(note)
                        + ",\"resendSeconds\":" + PasswordResetService.RESEND_SECONDS + "}");
            } else if ("verify".equals(action)) {
                reset.verifyCode(session, req.getParameter("code"));
                JsonUtil.write(resp, 200, "{\"ok\":true}");
            } else if ("reset".equals(action)) {
                String email = reset.resetPassword(session, req.getParameter("password"),
                        req.getParameter("confirmPassword"));
                JsonUtil.write(resp, 200, "{\"ok\":true,\"email\":" + JsonUtil.quote(email)
                        + ",\"message\":" + JsonUtil.quote("Đã đặt lại mật khẩu. Hãy đăng nhập bằng mật khẩu mới.") + "}");
            } else {
                JsonUtil.error(resp, 400, "Yêu cầu không hợp lệ.");
            }
        } catch (VerificationException e) {
            JsonUtil.error(resp, 400, e.getMessage());
        } catch (SQLException e) {
            throw new ServletException("Không thể đặt lại mật khẩu lúc này.", e);
        }
    }
}
