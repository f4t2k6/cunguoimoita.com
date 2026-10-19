package org.example.cunguoimoita.account.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.cunguoimoita.account.dao.AddressDAO;
import org.example.cunguoimoita.account.dao.UserDAO;
import org.example.cunguoimoita.account.model.Customer;
import org.example.cunguoimoita.common.util.CurrentUser;
import org.example.cunguoimoita.review.service.ReviewService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.regex.Pattern;

@WebServlet("/account/profile")
public class ProfileServlet extends HttpServlet {
    private static final Pattern PHONE = Pattern.compile("^[0-9+()\\-\\s]{8,20}$");
    private final UserDAO userDAO = new UserDAO();
    private final AddressDAO addressDAO = new AddressDAO();
    private final ReviewService reviewService = new ReviewService();   // F8: hạng thành viên hiện trong hồ sơ

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        loadProfile(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String fullName = trim(req.getParameter("fullName"));
        String phone = trim(req.getParameter("phone"));
        String payoutAccountRef = trim(req.getParameter("payoutAccountRef"));
        if (fullName.isBlank() || fullName.length() > 120 || !PHONE.matcher(phone).matches()
                || payoutAccountRef.length() > 255) {
            req.setAttribute("profileError", "Vui lòng kiểm tra lại họ tên, số điện thoại và tài khoản nhận tiền.");
            loadProfile(req, resp);
            return;
        }
        try {
            boolean updated = userDAO.updateCustomerProfile(CurrentUser.id(req), fullName,
                    phone, payoutAccountRef);
            if (!updated) {
                req.setAttribute("profileError", "Không thể cập nhật hồ sơ. Vui lòng tải lại trang.");
                loadProfile(req, resp);
                return;
            }
            req.getSession().setAttribute("fullName", fullName);
            resp.sendRedirect(req.getContextPath() + "/account/profile?saved=1");
        } catch (SQLException e) {
            throw new ServletException("Không thể lưu hồ sơ.", e);
        }
    }

    private void loadProfile(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String userId = CurrentUser.id(req);
        try {
            Customer customer = userDAO.findCustomerById(userId);
            if (customer == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            req.setAttribute("customer", customer);
            req.setAttribute("addresses", addressDAO.findByUserId(userId));
            // Hạng thành viên (cùng dữ liệu với /review/tier): progress, summary, reviews
            req.setAttribute("progress", reviewService.getTierProgress(userId));
            req.setAttribute("summary", reviewService.getSummary(userId));
            req.setAttribute("reviews", reviewService.getReviewsOf(userId));
            req.getRequestDispatcher("/WEB-INF/views/account/profile.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException("Không thể tải hồ sơ.", e);
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
