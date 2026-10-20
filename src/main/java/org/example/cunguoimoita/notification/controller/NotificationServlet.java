package org.example.cunguoimoita.notification.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.cunguoimoita.common.util.CurrentUser;
import org.example.cunguoimoita.common.util.JsonUtil;
import org.example.cunguoimoita.notification.model.Notification;
import org.example.cunguoimoita.notification.service.NotificationService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/** Thông báo: trang danh sách, số chưa đọc trên header, đánh dấu đã đọc. */
@WebServlet(urlPatterns = {"/notifications", "/notifications/open", "/notifications/count", "/notifications/list",
        "/notifications/read", "/notifications/read-all"})
public class NotificationServlet extends HttpServlet {

    private final NotificationService notificationService = new NotificationService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String userId = CurrentUser.id(req);
        if (userId == null) {
            String path = req.getServletPath();
            if ("/notifications".equals(path) || "/notifications/open".equals(path)) {
                CurrentUser.redirectToLogin(req, resp);
            } else {
                JsonUtil.error(resp, HttpServletResponse.SC_UNAUTHORIZED, "Cần đăng nhập");
            }
            return;
        }
        try {
            switch (req.getServletPath()) {
                case "/notifications/count" -> {
                    int[] counts = notificationService.badgeCounts(userId);
                    JsonUtil.write(resp, 200, "{\"ok\":true,\"count\":" + counts[0] + ",\"chat\":" + counts[1] + "}");
                }
                case "/notifications/list" -> JsonUtil.write(resp, 200, listJson(notificationService.list(userId, 20)));
                case "/notifications/open" -> {
                    String link = notificationService.open(userId, req.getParameter("id"));
                    // Chỉ chuyển tới đường dẫn trong trang (bắt đầu bằng một dấu "/"), tránh bị dẫn ra trang ngoài
                    boolean safe = link != null && link.startsWith("/") && !link.startsWith("//");
                    resp.sendRedirect(req.getContextPath() + (safe ? link : "/notifications"));
                }
                default -> {
                    req.setAttribute("notifications", notificationService.list(userId));
                    req.getRequestDispatcher("/WEB-INF/views/chat/notifications.jsp").forward(req, resp);
                }
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
            if ("/notifications/read".equals(req.getServletPath())) {
                notificationService.markRead(userId, req.getParameter("id"));
            } else {
                notificationService.markAllRead(userId);
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        String accept = req.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            JsonUtil.write(resp, 200, "{\"ok\":true}");
        } else {
            resp.sendRedirect(req.getContextPath() + "/notifications");
        }
    }

    private static String listJson(List<Notification> list) {
        StringBuilder json = new StringBuilder("{\"ok\":true,\"items\":[");
        for (int i = 0; i < list.size(); i++) {
            Notification n = list.get(i);
            if (i > 0) {
                json.append(',');
            }
            json.append("{\"id\":").append(JsonUtil.quote(n.getId()))
                .append(",\"type\":").append(JsonUtil.quote(n.getType().dbValue()))
                .append(",\"content\":").append(JsonUtil.quote(n.getContent()))
                .append(",\"read\":").append(n.isRead())
                .append(",\"createdAt\":").append(n.getCreatedEpochMs())
                .append('}');
        }
        return json.append("]}").toString();
    }
}
