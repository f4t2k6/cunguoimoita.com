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

/** Nút "Nhắn tin" trên trang chi tiết bài đăng: mở (hoặc tạo) cuộc trò chuyện với người bán. */
@WebServlet("/chat/start")
public class StartChatServlet extends HttpServlet {

    private final ChatService chatService = new ChatService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String userId = CurrentUser.id(req);
        if (userId == null) {
            CurrentUser.redirectToLogin(req, resp);
            return;
        }
        if (CurrentUser.isAdmin(req)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Tài khoản admin không dùng chức năng nhắn tin mua bán");
            return;
        }
        try {
            Conversation conv = chatService.start(req.getParameter("productId"), userId);
            resp.sendRedirect(req.getContextPath() + "/chat/conversation?id=" + conv.getId());
        } catch (IllegalArgumentException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }
}
