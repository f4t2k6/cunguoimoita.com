package org.example.cunguoimoita.common.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.cunguoimoita.account.service.AccountService;
import org.example.cunguoimoita.common.model.UserRole;
import org.example.cunguoimoita.common.util.CurrentUser;

import java.io.IOException;
import java.sql.SQLException;

@WebFilter("/account/*")
public class AccountAuthFilter implements Filter {
    private final AccountService accountService = new AccountService();

    @Override
    public void doFilter(jakarta.servlet.ServletRequest request,
                         jakarta.servlet.ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String userId = CurrentUser.id(req);
        if (userId == null) {
            // Nhớ trang đang mở để đăng nhập xong quay lại (giống LoginRequiredFilter)
            if ("GET".equals(req.getMethod())) {
                String query = req.getQueryString();
                req.getSession(true).setAttribute(CurrentUser.REDIRECT_AFTER_LOGIN,
                        req.getServletPath() + (req.getPathInfo() == null ? "" : req.getPathInfo())
                                + (query == null ? "" : "?" + query));
            }
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        HttpSession session = req.getSession(false);
        if (!UserRole.CUSTOMER.databaseValue().equals(session.getAttribute(CurrentUser.ROLE_KEY))) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        try {
            if (!accountService.isActive(userId)) {
                invalidate(session);
                resp.sendRedirect(req.getContextPath() + "/login?banned=1");
                return;
            }
        } catch (SQLException e) {
            throw new ServletException("Không thể xác minh trạng thái tài khoản.", e);
        }
        chain.doFilter(request, response);
    }

    private void invalidate(HttpSession session) {
        if (session != null) {
            session.invalidate();
        }
    }
}
