package org.example.cunguoimoita.common.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.cunguoimoita.account.dao.UserDAO;
import org.example.cunguoimoita.common.util.CurrentUser;

import java.io.IOException;
import java.sql.SQLException;

@WebFilter("/admin/*")
public class AdminAuthFilter implements Filter {
    private final UserDAO userDAO = new UserDAO();

    @Override
    public void doFilter(jakarta.servlet.ServletRequest request,
                         jakarta.servlet.ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        HttpSession session = req.getSession(false);
        String userId = CurrentUser.id(req);
        if (userId == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        try {
            if (!userDAO.isActiveAdmin(userId)) {
                if (!userDAO.isActive(userId)) {
                    session.invalidate();
                    resp.sendRedirect(req.getContextPath() + "/login?banned=1");
                } else {
                    // Tài khoản còn hoạt động nhưng không có quyền admin trong database.
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                }
                return;
            }
            // Database là nguồn xác thực quyền; đồng bộ session cũ hoặc role viết hoa.
            session.setAttribute("userRole", "admin");
        } catch (SQLException e) {
            throw new ServletException("Không thể xác minh trạng thái admin.", e);
        }
        chain.doFilter(request, response);
    }
}
