package org.example.cunguoimoita.common;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.cunguoimoita.common.util.CurrentUser;
import org.example.cunguoimoita.common.util.ImageStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * GET /uploads/<folder>/<uuid>.jpg → trả ảnh do ImageStore lưu (chat, khiếu nại, báo cáo).
 * Chỉ người đã đăng nhập mới xem được; tên file là uuid ngẫu nhiên nên không đoán được ảnh của người khác.
 */
@WebServlet("/uploads/*")
public class UploadImageServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (CurrentUser.id(req) == null) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        String path = req.getPathInfo();                       // "/chat/<uuid>.jpg"
        String[] parts = path == null ? new String[0] : path.substring(1).split("/");
        if (parts.length != 2 || !ImageStore.FOLDERS.contains(parts[0]) || !ImageStore.isValidFileName(parts[1])) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        Path file = ImageStore.directory(parts[0]).resolve(parts[1]);
        if (!Files.isRegularFile(file)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        resp.setContentType("image/jpeg");
        resp.setContentLengthLong(Files.size(file));
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("Cache-Control", "private, max-age=2592000, immutable");
        Files.copy(file, resp.getOutputStream());
    }
}
