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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@WebServlet("/account/avatar")
@MultipartConfig(maxFileSize = 5 * 1024 * 1024, maxRequestSize = 5 * 1024 * 1024)
public class AvatarServlet extends HttpServlet {
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        Part part = req.getPart("avatar");
        if (part == null || part.getSize() == 0 || part.getSize() > 5L * 1024 * 1024) {
            redirect(resp, req, "invalid");
            return;
        }

        BufferedImage image;
        try (var input = part.getInputStream()) {
            image = ImageIO.read(input);
        }
        if (image == null || image.getWidth() > 4096 || image.getHeight() > 4096) {
            redirect(resp, req, "invalid");
            return;
        }

        String base = System.getProperty("catalina.base");
        if (base == null || base.isBlank()) {
            throw new ServletException("Tomcat chưa thiết lập catalina.base để lưu ảnh đại diện.");
        }
        String fileName = UUID.randomUUID() + ".png";
        Path directory = Paths.get(base, "uploads", "avatars").toAbsolutePath().normalize();
        Files.createDirectories(directory);
        Path destination = directory.resolve(fileName).normalize();
        if (!destination.startsWith(directory) || !ImageIO.write(image, "png", destination.toFile())) {
            redirect(resp, req, "invalid");
            return;
        }

        try {
            if (!userDAO.updateAvatarPath(CurrentUser.id(req), "/avatar/" + fileName)) {
                Files.deleteIfExists(destination);
                redirect(resp, req, "saveFailed");
                return;
            }
            req.getSession().setAttribute("avatarPath", "/avatar/" + fileName);
            resp.sendRedirect(req.getContextPath() + "/account/profile?avatar=updated");
        } catch (java.sql.SQLException e) {
            Files.deleteIfExists(destination);
            throw new ServletException("Không thể lưu ảnh đại diện.", e);
        }
    }

    private void redirect(HttpServletResponse resp, HttpServletRequest req, String result)
            throws IOException {
        resp.sendRedirect(req.getContextPath() + "/account/profile?avatar=" + result);
    }
}
