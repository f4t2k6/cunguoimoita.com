package org.example.cunguoimoita.account.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.cunguoimoita.account.service.AccountService;
import org.example.cunguoimoita.common.util.CurrentUser;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/account/password")
public class ChangePasswordServlet extends HttpServlet {
    private final AccountService accountService = new AccountService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String oldPassword = req.getParameter("currentPassword");
        String newPassword = req.getParameter("newPassword");
        String confirmPassword = req.getParameter("confirmPassword");
        try {
            String result = switch (accountService.changePassword(CurrentUser.id(req), oldPassword, newPassword,
                    confirmPassword)) {
                case WEAK -> "weak";
                case MISMATCH -> "invalid";
                case INCORRECT -> "incorrect";
                case CHANGED -> {
                    req.changeSessionId();
                    yield "changed";
                }
            };
            resp.sendRedirect(req.getContextPath() + "/account/profile?password=" + result);
        } catch (SQLException e) {
            throw new ServletException("Không thể đổi mật khẩu.", e);
        }
    }
}
