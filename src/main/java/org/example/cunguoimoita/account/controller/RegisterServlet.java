package org.example.cunguoimoita.account.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.cunguoimoita.account.dao.UserDAO;
import org.example.cunguoimoita.common.util.PasswordUtil;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Locale;
import java.util.regex.Pattern;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PHONE = Pattern.compile("^[0-9+()\\-\\s]{8,20}$");
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.getRequestDispatcher("/register.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String fullName = trim(req.getParameter("fullName"));
        String email = trim(req.getParameter("email")).toLowerCase(Locale.ROOT);
        String phone = trim(req.getParameter("phone"));
        String password = req.getParameter("password");
        String confirmPassword = req.getParameter("confirmPassword");

        String error = validate(fullName, email, phone, password, confirmPassword);
        if (error != null) {
            showForm(req, resp, error);
            return;
        }

        try {
            userDAO.registerCustomer(email, PasswordUtil.hashPassword(password), fullName, phone);
            resp.sendRedirect(req.getContextPath() + "/login?registered=1");
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) {
                showForm(req, resp, "Email này đã được sử dụng.");
                return;
            }
            throw new ServletException("Không thể tạo tài khoản lúc này.", e);
        }
    }

    private String validate(String fullName, String email, String phone,
                            String password, String confirmPassword) {
        if (fullName.isBlank() || fullName.length() > 120) {
            return "Họ tên không được để trống và tối đa 120 ký tự.";
        }
        if (!EMAIL.matcher(email).matches() || email.length() > 254) {
            return "Vui lòng nhập email hợp lệ.";
        }
        if (!PHONE.matcher(phone).matches()) {
            return "Vui lòng nhập số điện thoại hợp lệ.";
        }
        if (password == null || password.length() < 8 || password.length() > 128) {
            return "Mật khẩu cần có từ 8 đến 128 ký tự.";
        }
        if (!password.equals(confirmPassword)) {
            return "Mật khẩu nhập lại chưa khớp.";
        }
        return null;
    }

    private void showForm(HttpServletRequest req, HttpServletResponse resp, String error)
            throws ServletException, IOException {
        req.setAttribute("errorMessage", error);
        req.getRequestDispatcher("/register.jsp").forward(req, resp);
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
