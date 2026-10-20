package org.example.cunguoimoita.account.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import org.example.cunguoimoita.account.service.AccountService;
import org.example.cunguoimoita.common.util.CurrentUser;
import org.example.cunguoimoita.common.util.MediaStore;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/account/avatar")
@MultipartConfig(maxFileSize = MediaStore.MAX_FILE_SIZE, maxRequestSize = MediaStore.MAX_FILE_SIZE)
public class AvatarServlet extends HttpServlet {
    private final AccountService accountService = new AccountService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        Part part = req.getPart("avatar");
        if (part == null || part.getSize() == 0 || part.getSize() > MediaStore.MAX_FILE_SIZE) {
            redirect(resp, req, "invalid");
            return;
        }

        try {
            String oldAvatar = (String) req.getSession().getAttribute(CurrentUser.AVATAR_KEY);
            String avatarUrl;
            try {
                avatarUrl = accountService.changeAvatar(CurrentUser.id(req), part, oldAvatar);
            } catch (IllegalArgumentException e) {
                redirect(resp, req, "invalid");
                return;
            }
            if (avatarUrl == null) {
                redirect(resp, req, "saveFailed");
                return;
            }
            req.getSession().setAttribute(CurrentUser.AVATAR_KEY, avatarUrl);
            resp.sendRedirect(req.getContextPath() + "/account/profile?avatar=updated");
        } catch (SQLException e) {
            throw new ServletException("Không thể lưu ảnh đại diện.", e);
        }
    }

    private void redirect(HttpServletResponse resp, HttpServletRequest req, String result)
            throws IOException {
        resp.sendRedirect(req.getContextPath() + "/account/profile?avatar=" + result);
    }
}