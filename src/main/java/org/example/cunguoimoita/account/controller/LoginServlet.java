package org.example.cunguoimoita.account.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.cunguoimoita.account.dao.UserDAO;
import org.example.cunguoimoita.account.model.User;
import org.example.cunguoimoita.common.model.AccountStatus;
import org.example.cunguoimoita.common.model.UserRole;
import org.example.cunguoimoita.common.util.PasswordUtil;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Locale;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
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

        try {
            User user = userDAO.findByEmail(email.trim().toLowerCase(Locale.ROOT));
            if (user == null || !PasswordUtil.verifyPassword(password, user.getPasswordHash())) {
                showForm(req, resp, "Email hoặc mật khẩu không đúng.");
                return;
            }
            if (user.getStatus() == AccountStatus.BANNED) {
                showForm(req, resp, "Tài khoản của bạn đang bị khóa.");
                return;
            }

            HttpSession session = req.getSession(true);
            req.changeSessionId();
            session.setAttribute("userId", user.getId());
            session.setAttribute("userRole", user.getRole().databaseValue());
            session.setAttribute("fullName", user.getFullName());
            session.setAttribute("avatarPath", user.getAvatarPath());
            if (user.getRole() == UserRole.ADMIN) {
                resp.sendRedirect(req.getContextPath() + "/admin/dashboard");
            } else {
                resp.sendRedirect(req.getContextPath() + "/home");
            }
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
