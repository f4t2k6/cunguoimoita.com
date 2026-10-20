package org.example.cunguoimoita.account.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import org.example.cunguoimoita.account.dao.UserDAO;
import org.example.cunguoimoita.common.util.CurrentUser;
import org.example.cunguoimoita.common.util.MediaStore;

import java.io.IOException;

@WebServlet("/account/avatar")
@MultipartConfig(maxFileSize = MediaStore.MAX_FILE_SIZE, maxRequestSize = MediaStore.MAX_FILE_SIZE)
public class AvatarServlet extends HttpServlet {
    private final UserDAO userDAO = new UserDAO();
    private final MediaStore mediaStore = new MediaStore();

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
            String avatarUrl;
            try {
                // Đọc ảnh, thu nhỏ còn tối đa 512px, nén JPEG rồi lưu vào bảng media
                avatarUrl = mediaStore.save(part, CurrentUser.id(req), 512);
            } catch (IllegalArgumentException e) {
                redirect(resp, req, "invalid");
                return;
            }

            String oldAvatar = (String) req.getSession().getAttribute(CurrentUser.AVATAR_KEY);
            if (!userDAO.updateAvatarPath(CurrentUser.id(req), avatarUrl)) {
                mediaStore.delete(avatarUrl);
                redirect(resp, req, "saveFailed");
                return;
            }
            mediaStore.delete(oldAvatar);   // bỏ ảnh đại diện cũ trong bảng media (đường dẫn kiểu cũ thì bị bỏ qua)
            req.getSession().setAttribute(CurrentUser.AVATAR_KEY, avatarUrl);
            resp.sendRedirect(req.getContextPath() + "/account/profile?avatar=updated");
        } catch (java.sql.SQLException e) {
            throw new ServletException("Không thể lưu ảnh đại diện.", e);
        }
    }

    private void redirect(HttpServletResponse resp, HttpServletRequest req, String result)
            throws IOException {
        resp.sendRedirect(req.getContextPath() + "/account/profile?avatar=" + result);
    }
}