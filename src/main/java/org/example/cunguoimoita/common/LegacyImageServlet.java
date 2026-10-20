package org.example.cunguoimoita.common;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.cunguoimoita.common.util.CurrentUser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

/**
 * Trả ảnh cũ lưu trên đĩa ở &lt;catalina.base&gt;/uploads, từ trước khi ảnh được lưu trong DB (/media/*).
 * Vẫn cần vì DB còn bài đăng và avatar trỏ tới các đường dẫn này; ảnh mới không ghi theo cách cũ nữa.
 */
@WebServlet({"/uploads/*", "/product-image/*", "/avatar/*"})
public class LegacyImageServlet extends HttpServlet {

    private static final Set<String> UPLOAD_FOLDERS = Set.of("chat", "disputes", "reports");
    private static final String JPEG_NAME = "[0-9a-f-]{36}\\.jpg";
    private static final String AVATAR_NAME = "[0-9a-fA-F-]{36}\\.png";
    private static final String LONG_CACHE = "max-age=2592000, immutable";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String name = req.getPathInfo() == null ? "" : req.getPathInfo().substring(1);
        switch (req.getServletPath()) {
            case "/uploads" -> serveUpload(req, resp, name);
            case "/product-image" -> serve(resp, name, JPEG_NAME, "products", "image/jpeg", "public, " + LONG_CACHE);
            default -> serve(resp, name, AVATAR_NAME, "avatars", "image/png", "public, max-age=86400");
        }
    }

    // Ảnh chat, khiếu nại, báo cáo chỉ người đã đăng nhập mới xem được.
    private void serveUpload(HttpServletRequest req, HttpServletResponse resp, String path) throws IOException {
        if (CurrentUser.id(req) == null) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        String[] parts = path.split("/");
        if (parts.length != 2 || !UPLOAD_FOLDERS.contains(parts[0])) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        serve(resp, parts[1], JPEG_NAME, parts[0], "image/jpeg", "private, " + LONG_CACHE);
    }

    // Tên file phải khớp mẫu uuid nên không thể dùng ../ để đọc file ngoài thư mục ảnh.
    private void serve(HttpServletResponse resp, String fileName, String namePattern, String folder,
                       String contentType, String cacheControl) throws IOException {
        String base = System.getProperty("catalina.base");
        if (!fileName.matches(namePattern) || base == null || base.isBlank()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        Path file = Paths.get(base, "uploads", folder, fileName).toAbsolutePath().normalize();
        if (!Files.isRegularFile(file)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        resp.setContentType(contentType);
        resp.setContentLengthLong(Files.size(file));
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("Cache-Control", cacheControl);
        Files.copy(file, resp.getOutputStream());
    }
}
