package org.example.cunguoimoita.account.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.cunguoimoita.account.dao.PublicSellerProfileDAO;
import org.example.cunguoimoita.account.model.PublicSellerProfile;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/users/profile")
public class PublicSellerProfileServlet extends HttpServlet {
    private final PublicSellerProfileDAO profileDAO = new PublicSellerProfileDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String userId = req.getParameter("id");
        if (userId == null || !userId.matches("[0-9a-fA-F-]{36}")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        try {
            PublicSellerProfile profile = profileDAO.findByUserId(userId);
            if (profile == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            req.setAttribute("seller", profile);
            req.getRequestDispatcher("/WEB-INF/views/account/public-profile.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException("Không thể tải hồ sơ người bán.", e);
        }
    }
}
