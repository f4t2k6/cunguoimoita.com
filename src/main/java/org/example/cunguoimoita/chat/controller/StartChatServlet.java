package org.example.cunguoimoita.chat.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.cunguoimoita.chat.model.Conversation;
import org.example.cunguoimoita.chat.service.ChatService;
import org.example.cunguoimoita.common.util.CurrentUser;

import java.io.IOException;
import java.sql.SQLException;

/**
 * POST /chat/start  — nút "Nhắn tin" trên trang chi tiết bài đăng (F3 của TV2 đặt form này).
 *      Field: productId, sellerId
 *      → chuyển tới /chat/conversation?id=...
 * TODO: khi có ProductService (TV2) thì tự lấy sellerId từ productId thay vì nhận từ form.
 */
@WebServlet("/chat/start")
public class StartChatServlet extends HttpServlet {

    private final ChatService chatService = new ChatService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String userId = CurrentUser.id(req);
        if (userId == null) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        try {
            Conversation conv = chatService.start(req.getParameter("productId"), userId, req.getParameter("sellerId"));
            resp.sendRedirect(req.getContextPath() + "/chat/conversation?id=" + conv.getId());
        } catch (IllegalArgumentException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }
}
