package org.example.cunguoimoita.account.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.cunguoimoita.account.model.User;
import org.example.cunguoimoita.account.service.AccountService;
import org.example.cunguoimoita.account.service.LoginAttemptLimiter;
import org.example.cunguoimoita.common.util.CurrentUser;
import org.example.cunguoimoita.common.model.AccountStatus;
import org.example.cunguoimoita.common.model.UserRole;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    private final AccountService accountService = new AccountService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // Lỗi đăng nhập Google/Facebook (SocialLoginServlet) được gửi qua session
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute("loginError") != null) {
            req.setAttribute("errorMessage", session.getAttribute("loginError"));
            session.removeAttribute("loginError");
        }
        req.getRequestDispatcher("/login.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String email = req.getParameter("email");
        String password = req.getParameter("password");
        if (email == null || password == null || email.isBlank() || password.isEmpty()) {
            showForm(req, resp, "Vui lòng nhập email và mật khẩu.");
            return;
        }

        String key = AccountService.normalizeEmail(email);
        LoginAttemptLimiter limiter = LoginAttemptLimiter.getInstance();
        long locked = limiter.secondsLocked(key);
        if (locked > 0) {
            showForm(req, resp, "Bạn đã nhập sai " + LoginAttemptLimiter.MAX_FAILURES
                    + " lần. Vui lòng thử lại sau " + ((locked + 59) / 60) + " phút.");
            return;
        }

        try {
            User user = accountService.authenticate(key, password);
            if (user == null) {
                int left = limiter.recordFailure(key);
                showForm(req, resp, left > 0
                        ? "Email hoặc mật khẩu không đúng. Bạn còn " + left + " lần thử."
                        : "Bạn đã nhập sai " + LoginAttemptLimiter.MAX_FAILURES + " lần. Vui lòng thử lại sau "
                        + LoginAttemptLimiter.LOCK_MINUTES + " phút.");
                return;
            }
            limiter.recordSuccess(key);
            if (user.getStatus() == AccountStatus.BANNED) {
                showForm(req, resp, "Tài khoản của bạn đang bị khóa.");
                return;
            }

            String wanted = CurrentUser.logIn(req, user.getId(), user.getRole().databaseValue(),
                    user.getFullName(), user.getAvatarPath());
            CurrentUser.redirectAfterLogin(req, resp, user.getRole() == UserRole.ADMIN, wanted);
        } catch (SQLException e) {
            throw new ServletException("Không thể đăng nhập lúc này.", e);
        }
    }

    private void showForm(HttpServletRequest req, HttpServletResponse resp, String error)
            throws ServletException, IOException {
        req.setAttribute("errorMessage", error);
        req.getRequestDispatcher("/login.jsp").forward(req, resp);
    }
}
