package org.example.cunguoimoita.account.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.cunguoimoita.account.service.EmailVerificationService;
import org.example.cunguoimoita.account.service.VerificationException;
import org.example.cunguoimoita.common.util.MailSender;
import org.example.cunguoimoita.common.util.PasswordUtil;
import org.example.cunguoimoita.common.util.Texts;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Locale;
import java.util.regex.Pattern;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PHONE = Pattern.compile("^[0-9+()\\-\\s]{8,20}$");
    private EmailVerificationService verification;

    @Override
    public void init() {
        verification = new EmailVerificationService(new MailSender(getServletContext()));
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.getRequestDispatcher("/register.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String fullName = Texts.trimToEmpty(req.getParameter("fullName"));
        String email = Texts.trimToEmpty(req.getParameter("email")).toLowerCase(Locale.ROOT);
        String phone = Texts.trimToEmpty(req.getParameter("phone"));
        String password = req.getParameter("password");
        String confirmPassword = req.getParameter("confirmPassword");

        String error = validate(fullName, email, phone, password, confirmPassword);
        if (error != null) {
            showForm(req, resp, error);
            return;
        }

        // Chưa tạo tài khoản ngay: gửi mã về email, nhập đúng mã ở /register/verify mới tạo
        try {
            verification.start(fullName, email, phone, password);
        } catch (VerificationException e) {
            showForm(req, resp, e.getMessage());
            return;
        } catch (SQLException e) {
            throw new ServletException("Không thể tạo tài khoản lúc này.", e);
        }
        req.getSession(true).setAttribute(VerifyEmailServlet.PENDING_EMAIL, email);
        resp.sendRedirect(req.getContextPath() + "/register/verify");
    }

    private String validate(String fullName, String email, String phone,
                            String password, String confirmPassword) {
        if (fullName.isBlank() || fullName.length() > 100) {
            return "Họ tên không được để trống và tối đa 100 ký tự.";
        }
        if (!EMAIL.matcher(email).matches() || email.length() > 254) {
            return "Vui lòng nhập email hợp lệ.";
        }
        if (!PHONE.matcher(phone).matches()) {
            return "Vui lòng nhập số điện thoại hợp lệ.";
        }
        String weak = PasswordUtil.checkStrength(password);
        if (weak != null) {
            return weak;
        }
        if (!password.equals(confirmPassword)) {
            return "Mật khẩu nhập lại chưa khớp.";
        }
        return null;
    }

    private void showForm(HttpServletRequest req, HttpServletResponse resp, String error)
            throws ServletException, IOException {
        req.setAttribute("errorMessage", error);
        // Giữ lại thông tin đã nhập (trừ mật khẩu) để không phải gõ lại
        req.setAttribute("fullName", Texts.trimToEmpty(req.getParameter("fullName")));
        req.setAttribute("email", Texts.trimToEmpty(req.getParameter("email")));
        req.setAttribute("phone", Texts.trimToEmpty(req.getParameter("phone")));
        req.getRequestDispatcher("/register.jsp").forward(req, resp);
    }
}
