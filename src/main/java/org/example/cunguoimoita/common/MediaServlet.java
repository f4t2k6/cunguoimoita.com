package org.example.cunguoimoita.common;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.cunguoimoita.common.dao.MediaDAO;

import java.io.IOException;
import java.sql.SQLException;

/** GET /media/<id> → đọc ảnh từ bảng media rồi trả cho trình duyệt. */
@WebServlet("/media/*")
public class MediaServlet extends HttpServlet {

    private final MediaDAO mediaDAO = new MediaDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();
        String id = path == null ? null : path.substring(1);
        if (id == null || !id.matches("[0-9a-f-]{36}")) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        try {
            MediaDAO.Media media = mediaDAO.find(id);
            if (media == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            resp.setContentType(media.contentType());
            resp.setContentLength(media.data().length);
            resp.setHeader("X-Content-Type-Options", "nosniff");
            // id mới cho mỗi lần upload nên cho trình duyệt giữ lâu, đỡ phải hỏi Neon lại
            resp.setHeader("Cache-Control", "public, max-age=2592000, immutable");
            resp.getOutputStream().write(media.data());
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }
}