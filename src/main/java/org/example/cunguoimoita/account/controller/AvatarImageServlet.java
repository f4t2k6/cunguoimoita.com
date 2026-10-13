package org.example.cunguoimoita.account.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@WebServlet("/avatar/*")
public class AvatarImageServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        String fileName = req.getPathInfo();
        if (fileName == null || !fileName.matches("/[0-9a-fA-F-]{36}\\.png")) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String base = System.getProperty("catalina.base");
        if (base == null || base.isBlank()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        Path directory = Paths.get(base, "uploads", "avatars").toAbsolutePath().normalize();
        Path image = directory.resolve(fileName.substring(1)).normalize();
        if (!image.startsWith(directory) || !Files.isRegularFile(image)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        resp.setContentType("image/png");
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("Cache-Control", "public, max-age=86400");
        Files.copy(image, resp.getOutputStream());
    }
}
