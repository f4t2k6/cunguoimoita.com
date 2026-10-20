package org.example.cunguoimoita.chat.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.cunguoimoita.chat.service.ChatService;
import org.example.cunguoimoita.common.util.CurrentUser;

import java.io.IOException;
import java.sql.SQLException;

/** Hộp thư: các cuộc trò chuyện của người dùng. */
@WebServlet("/chat/inbox")
public class InboxServlet extends HttpServlet {

    private final ChatService chatService = new ChatService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String userId = CurrentUser.id(req);
        if (userId == null) {
            CurrentUser.redirectToLogin(req, resp);
            return;
        }
        try {
            req.setAttribute("conversations", chatService.inbox(userId));
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        req.setAttribute("userId", userId);
        req.getRequestDispatcher("/WEB-INF/views/chat/inbox.jsp").forward(req, resp);
    }
}
