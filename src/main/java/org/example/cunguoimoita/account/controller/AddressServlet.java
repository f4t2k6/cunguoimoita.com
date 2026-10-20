package org.example.cunguoimoita.account.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.cunguoimoita.account.dao.AddressDAO;
import org.example.cunguoimoita.common.util.CurrentUser;
import org.example.cunguoimoita.common.util.Texts;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/account/addresses")
public class AddressServlet extends HttpServlet {
    private final AddressDAO addressDAO = new AddressDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String userId = CurrentUser.id(req);
        String action = Texts.trimToEmpty(req.getParameter("action"));
        String id = Texts.trimToEmpty(req.getParameter("id"));
        try {
            boolean success;
            switch (action) {
                case "add" -> {
                    if (!validAddress(req)) {
                        redirect(resp, req, "invalidAddress");
                        return;
                    }
                    addressDAO.add(userId, req.getParameter("label"), req.getParameter("addressLine"),
                            req.getParameter("ward"), req.getParameter("district"),
                            req.getParameter("province"));
                    success = true;
                }
                case "update" -> {
                    if (id.isBlank() || !validAddress(req)) {
                        redirect(resp, req, "invalidAddress");
                        return;
                    }
                    success = addressDAO.update(id, userId, req.getParameter("label"),
                            req.getParameter("addressLine"), req.getParameter("ward"),
                            req.getParameter("district"), req.getParameter("province"));
                }
                case "default" -> success = !id.isBlank() && addressDAO.setDefault(id, userId);
                case "delete" -> success = !id.isBlank() && addressDAO.delete(id, userId);
                default -> {
                    redirect(resp, req, "invalidAction");
                    return;
                }
            }
            redirect(resp, req, success ? "addressSaved" : "addressNotFound");
        } catch (SQLException e) {
            throw new ServletException("Không thể cập nhật sổ địa chỉ.", e);
        }
    }

    private boolean validAddress(HttpServletRequest req) {
        // Người nhận và số điện thoại lấy theo tài khoản (database chung không lưu riêng cho địa chỉ)
        return length(req.getParameter("addressLine"), 1, 255)
                && length(req.getParameter("province"), 1, 100)
                && length(req.getParameter("label"), 0, 50)
                // Địa chỉ 2 cấp (từ 01/07/2025): bắt buộc phường/xã, không còn quận/huyện (cột district để trống)
                && length(req.getParameter("ward"), 1, 100)
                && length(req.getParameter("district"), 0, 100);
    }

    private boolean length(String value, int min, int max) {
        String clean = Texts.trimToEmpty(value);
        return clean.length() >= min && clean.length() <= max;
    }

    private void redirect(HttpServletResponse resp, HttpServletRequest req, String result)
            throws IOException {
        resp.sendRedirect(req.getContextPath() + "/account/profile?address=" + result);
    }
}
