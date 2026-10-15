package org.example.cunguoimoita.account.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.cunguoimoita.account.model.User;
import org.example.cunguoimoita.account.service.EmailVerificationService;
import org.example.cunguoimoita.account.service.VerificationException;
import org.example.cunguoimoita.common.util.MailSender;

import java.io.IOException;
import java.sql.SQLException;

/** Bước 2 của đăng ký: nhập mã 6 số đã gửi về email. Đúng mã thì tạo tài khoản và đăng nhập luôn. */
@WebServlet("/register/verify")
public class VerifyEmailServlet extends HttpServlet {
    /** Email đang chờ xác nhận, lưu trong session sau bước đăng ký. */
    public static final String PENDING_EMAIL = "pendingEmail";

    private EmailVerificationService verification;

    @Override
    public void init() {
        verification = new EmailVerificationService(new MailSender(getServletContext()));
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String email = pendingEmail(req);
        if (email == null) {
            resp.sendRedirect(req.getContextPath() + "/register");
            return;
        }
        if ("1".equals(req.getParameter("sent"))) {
            req.setAttribute("successMessage", "Đã gửi mã mới, hãy kiểm tra hộp thư (cả mục Spam).");
        }
        showForm(req, resp, email);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String email = pendingEmail(req);
        if (email == null) {
            resp.sendRedirect(req.getContextPath() + "/register");
            return;
        }
        try {
            if ("resend".equals(req.getParameter("action"))) {
                verification.resend(email);
                resp.sendRedirect(req.getContextPath() + "/register/verify?sent=1");
                return;
            }
            User user = verification.verify(email, req.getParameter("code"));
            logIn(req, user);
            resp.sendRedirect(req.getContextPath() + "/home");
        } catch (VerificationException e) {
            req.setAttribute("errorMessage", e.getMessage());
            showForm(req, resp, email);
        } catch (SQLException e) {
            throw new ServletException("Không thể xác nhận email lúc này.", e);
        }
    }

    /** Giống LoginServlet: đổi session id rồi lưu thông tin người dùng. */
    private void logIn(HttpServletRequest req, User user) {
        HttpSession session = req.getSession(true);
        req.changeSessionId();
        session.removeAttribute(PENDING_EMAIL);
        session.setAttribute("userId", user.getId());
        session.setAttribute("userRole", user.getRole().databaseValue());
        session.setAttribute("fullName", user.getFullName());
        session.setAttribute("avatarPath", user.getAvatarPath());
    }

    private void showForm(HttpServletRequest req, HttpServletResponse resp, String email)
            throws ServletException, IOException {
        req.setAttribute("email", email);
        req.setAttribute("validMinutes", EmailVerificationService.VALID_MINUTES);
        req.setAttribute("mailConfigured", verification.isMailConfigured());
        req.getRequestDispatcher("/verify-email.jsp").forward(req, resp);
    }

    private String pendingEmail(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session == null ? null : (String) session.getAttribute(PENDING_EMAIL);
    }
}
