package org.example.cunguoimoita.product.controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.cunguoimoita.product.service.ProductImageStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** GET /product-image/<uuid>.jpg → trả file ảnh bài đăng đã lưu trên đĩa. */
@WebServlet("/product-image/*")
public class ProductImageServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        String name = path == null ? null : path.substring(1);
        if (!ProductImageStore.isValidFileName(name)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        Path file = ProductImageStore.directory().resolve(name);
        if (!Files.isRegularFile(file)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        resp.setContentType("image/jpeg");
        resp.setContentLengthLong(Files.size(file));
        resp.setHeader("X-Content-Type-Options", "nosniff");
        // Tên file là uuid mới mỗi lần tải lên nên cho trình duyệt giữ lâu
        resp.setHeader("Cache-Control", "public, max-age=2592000, immutable");
        Files.copy(file, resp.getOutputStream());
    }
}
