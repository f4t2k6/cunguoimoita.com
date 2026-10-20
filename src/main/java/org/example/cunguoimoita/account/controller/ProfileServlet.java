package org.example.cunguoimoita.account.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.cunguoimoita.account.model.Customer;
import org.example.cunguoimoita.account.service.AccountService;
import org.example.cunguoimoita.common.util.CurrentUser;
import org.example.cunguoimoita.common.util.Texts;
import org.example.cunguoimoita.review.service.ReviewService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.regex.Pattern;

@WebServlet("/account/profile")
public class ProfileServlet extends HttpServlet {
    private static final Pattern PHONE = Pattern.compile("^[0-9+()\\-\\s]{8,20}$");
    private final AccountService accountService = new AccountService();
    private final ReviewService reviewService = new ReviewService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        loadProfile(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String fullName = Texts.trimToEmpty(req.getParameter("fullName"));
        String phone = Texts.trimToEmpty(req.getParameter("phone"));
        String payoutAccountRef = Texts.trimToEmpty(req.getParameter("payoutAccountRef"));
        if (fullName.isBlank() || fullName.length() > 120 || !PHONE.matcher(phone).matches()
                || payoutAccountRef.length() > 255) {
            req.setAttribute("profileError", "Vui lòng kiểm tra lại họ tên, số điện thoại và tài khoản nhận tiền.");
            loadProfile(req, resp);
            return;
        }
        try {
            boolean updated = accountService.updateProfile(CurrentUser.id(req), fullName,
                    phone, payoutAccountRef);
            if (!updated) {
                req.setAttribute("profileError", "Không thể cập nhật hồ sơ. Vui lòng tải lại trang.");
                loadProfile(req, resp);
                return;
            }
            req.getSession().setAttribute(CurrentUser.NAME_KEY, fullName);
            resp.sendRedirect(req.getContextPath() + "/account/profile?saved=1");
        } catch (SQLException e) {
            throw new ServletException("Không thể lưu hồ sơ.", e);
        }
    }

    private void loadProfile(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String userId = CurrentUser.id(req);
        try {
            Customer customer = accountService.findCustomer(userId);
            if (customer == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            req.setAttribute("customer", customer);
            req.setAttribute("addresses", accountService.getAddresses(userId));
            // Hạng thành viên (cùng dữ liệu với /review/tier): progress, summary, reviews
            req.setAttribute("progress", reviewService.getTierProgress(userId));
            req.setAttribute("summary", reviewService.getSummary(userId));
            req.setAttribute("reviews", reviewService.getReviewsOf(userId));
            req.getRequestDispatcher("/WEB-INF/views/account/profile.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException("Không thể tải hồ sơ.", e);
        }
    }
}
