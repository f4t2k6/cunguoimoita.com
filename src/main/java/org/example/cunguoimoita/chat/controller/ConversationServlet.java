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
 * GET /chat/conversation?id=... → khung chat. Tin nhắn do chat.js tải qua /chat/messages.
 *     Attribute gửi sang JSP: conversation (Conversation), userId
 */
@WebServlet("/chat/conversation")
public class ConversationServlet extends HttpServlet {

    private final ChatService chatService = new ChatService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String userId = CurrentUser.id(req);
        if (userId == null) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        try {
            Conversation conv = chatService.getConversation(req.getParameter("id"), userId);
            req.setAttribute("conversation", conv);
        } catch (IllegalArgumentException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
            return;
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        req.setAttribute("userId", userId);
        req.getRequestDispatcher("/WEB-INF/views/chat/conversation.jsp").forward(req, resp);
    }
}
