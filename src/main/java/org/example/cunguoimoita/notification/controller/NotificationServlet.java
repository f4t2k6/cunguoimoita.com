package org.example.cunguoimoita.notification.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.cunguoimoita.common.util.CurrentUser;
import org.example.cunguoimoita.common.util.JsonUtil;
import org.example.cunguoimoita.notification.service.NotificationService;

import java.io.IOException;
import java.sql.SQLException;

/**
 *  GET  /notifications             danh sách thông báo. Attribute gửi sang JSP: notifications (List&lt;Notification&gt;)
 *  GET  /notifications/open?id=    đánh dấu đã đọc rồi chuyển tới linkUrl của thông báo
 *  GET  /notifications/count       JSON số chưa đọc (chuông trên header)
 *  POST /notifications/read-all    đánh dấu đã đọc tất cả
 */
@WebServlet(urlPatterns = {"/notifications", "/notifications/open", "/notifications/count", "/notifications/read-all"})
public class NotificationServlet extends HttpServlet {

    private final NotificationService notificationService = new NotificationService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String userId = CurrentUser.id(req);
        if (userId == null) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Cần đăng nhập (tạm thời thêm ?as=<userId>)");
            return;
        }
        try {
            switch (req.getServletPath()) {
                case "/notifications/count":
                    JsonUtil.write(resp, 200, "{\"ok\":true,\"count\":" + notificationService.unreadCount(userId) + "}");
                    return;
                case "/notifications/open":
                    String link = notificationService.open(userId, req.getParameter("id"));
                    resp.sendRedirect(req.getContextPath() + (link == null ? "/notifications" : link));
                    return;
                default:
                    req.setAttribute("notifications", notificationService.list(userId));
                    req.getRequestDispatcher("/WEB-INF/views/chat/notifications.jsp").forward(req, resp);
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String userId = CurrentUser.id(req);
        if (userId == null) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        try {
            notificationService.markAllRead(userId);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        resp.sendRedirect(req.getContextPath() + "/notifications");
    }
}
