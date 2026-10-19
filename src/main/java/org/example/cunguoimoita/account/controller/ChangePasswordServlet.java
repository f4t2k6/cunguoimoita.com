package org.example.cunguoimoita.account.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.cunguoimoita.account.dao.UserDAO;
import org.example.cunguoimoita.account.model.User;
import org.example.cunguoimoita.common.util.CurrentUser;
import org.example.cunguoimoita.common.util.PasswordUtil;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/account/password")
public class ChangePasswordServlet extends HttpServlet {
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String oldPassword = req.getParameter("currentPassword");
        String newPassword = req.getParameter("newPassword");
        String confirmPassword = req.getParameter("confirmPassword");
        if (PasswordUtil.checkStrength(newPassword) != null) {
            resp.sendRedirect(req.getContextPath() + "/account/profile?password=weak");
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            resp.sendRedirect(req.getContextPath() + "/account/profile?password=invalid");
            return;
        }
        try {
            User user = userDAO.findById(CurrentUser.id(req));
            if (user == null || !PasswordUtil.verifyPassword(oldPassword, user.getPasswordHash())) {
                resp.sendRedirect(req.getContextPath() + "/account/profile?password=incorrect");
                return;
            }
            userDAO.updatePasswordHash(user.getId(), PasswordUtil.hashPassword(newPassword));
            req.changeSessionId();
            resp.sendRedirect(req.getContextPath() + "/account/profile?password=changed");
        } catch (SQLException e) {
            throw new ServletException("Không thể đổi mật khẩu.", e);
        }
    }
}
