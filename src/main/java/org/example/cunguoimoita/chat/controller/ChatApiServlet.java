package org.example.cunguoimoita.chat.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.cunguoimoita.chat.model.Message;
import org.example.cunguoimoita.chat.model.PriceOffer;
import org.example.cunguoimoita.chat.service.ChatService;
import org.example.cunguoimoita.chat.service.ChatService.ChatUpdate;
import org.example.cunguoimoita.common.util.CurrentUser;
import org.example.cunguoimoita.common.util.JsonUtil;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.List;

/**
 * API JSON cho chat.js (khung chat không tải lại trang).
 *  GET  /chat/messages?id=&after=   tin nhắn mới hơn mốc after (epoch ms), after bỏ trống để lấy hết.
 *                                   Có tin mới thì kèm "offers" (mọi offer của cuộc trò chuyện) để cập nhật khung giá cũ.
 *  GET  /chat/unread                số tin chưa đọc (icon tin nhắn trên header)
 *  POST /chat/send                  conversationId, content
 *  POST /chat/offer                 action=make (conversationId, amount) | accept | decline (offerId) | counter (offerId, amount)
 * Lỗi trả {"ok":false,"error":"..."}.
 */
@WebServlet(urlPatterns = {"/chat/messages", "/chat/unread", "/chat/send", "/chat/offer"})
public class ChatApiServlet extends HttpServlet {

    private final ChatService chatService = new ChatService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String userId = CurrentUser.id(req);
        if (userId == null) {
            JsonUtil.error(resp, HttpServletResponse.SC_UNAUTHORIZED, "Cần đăng nhập");
            return;
        }
        try {
            if ("/chat/unread".equals(req.getServletPath())) {
                JsonUtil.write(resp, 200, "{\"ok\":true,\"count\":" + chatService.unreadCount(userId) + "}");
                return;
            }
            ChatUpdate update = chatService.loadMessages(req.getParameter("id"), userId, afterParam(req));
            StringBuilder json = new StringBuilder("{\"ok\":true,\"messages\":[");
            List<Message> messages = update.messages();
            for (int i = 0; i < messages.size(); i++) {
                if (i > 0) {
                    json.append(',');
                }
                appendMessage(json, messages.get(i), userId);
            }
            json.append("],\"offers\":[");
            List<PriceOffer> offers = update.offers();
            for (int i = 0; i < offers.size(); i++) {
                if (i > 0) {
                    json.append(',');
                }
                appendOffer(json, offers.get(i), userId);
            }
            JsonUtil.write(resp, 200, json.append("]}").toString());
        } catch (IllegalArgumentException e) {
            JsonUtil.error(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String userId = CurrentUser.id(req);
        if (userId == null) {
            JsonUtil.error(resp, HttpServletResponse.SC_UNAUTHORIZED, "Cần đăng nhập");
            return;
        }
        try {
            if ("/chat/send".equals(req.getServletPath())) {
                chatService.sendMessage(req.getParameter("conversationId"), userId, req.getParameter("content"));
            } else {
                String offerId = req.getParameter("offerId");
                switch (String.valueOf(req.getParameter("action"))) {
                    case "make":
                        chatService.makeOffer(req.getParameter("conversationId"), userId, amount(req));
                        break;
                    case "accept":
                        chatService.acceptOffer(offerId, userId);
                        break;
                    case "decline":
                        chatService.declineOffer(offerId, userId);
                        break;
                    case "counter":
                        chatService.counterOffer(offerId, userId, amount(req));
                        break;
                    default:
                        throw new IllegalArgumentException("Thao tác không hợp lệ");
                }
            }
            JsonUtil.write(resp, 200, "{\"ok\":true}");
        } catch (IllegalArgumentException | IllegalStateException e) {
            JsonUtil.error(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private static double amount(HttpServletRequest req) {
        try {
            double value = Double.parseDouble(req.getParameter("amount").trim());
            // Chặn NaN/Infinity và số vượt NUMERIC(12,2) của price_offers.amount
            if (!Double.isFinite(value) || value <= 0 || value >= 1e10) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException | NullPointerException e) {
            throw new IllegalArgumentException("Số tiền không hợp lệ");
        }
    }

    /**
     * Mốc after (epoch ms) → Timestamp. Cộng thêm 0,999999 ms để tin cũ lưu lẻ micro giây
     * (trước khi làm tròn tới ms) không bị trả lại ở mỗi lần hỏi.
     */
    private static Timestamp afterParam(HttpServletRequest req) {
        String after = req.getParameter("after");
        if (after == null || after.isBlank()) {
            return null;
        }
        try {
            Timestamp ts = new Timestamp(Long.parseLong(after));
            ts.setNanos(ts.getNanos() + 999_999);
            return ts;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Mốc thời gian không hợp lệ");
        }
    }

    private static long epochMs(java.time.LocalDateTime t) {
        return t.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private static void appendMessage(StringBuilder json, Message m, String userId) {
        json.append("{\"id\":").append(JsonUtil.quote(m.getId()))
            .append(",\"mine\":").append(userId.equals(m.getSenderId()))
            .append(",\"type\":").append(JsonUtil.quote(m.getType().dbValue()))
            .append(",\"sentAt\":").append(epochMs(m.getSentAt()));
        PriceOffer o = m.getOffer();
        if (o != null) {
            json.append(",\"offer\":");
            appendOffer(json, o, userId);
        } else {
            json.append(",\"content\":").append(JsonUtil.quote(m.getContent()));
        }
        json.append('}');
    }

    private static void appendOffer(StringBuilder json, PriceOffer o, String userId) {
        json.append("{\"id\":").append(JsonUtil.quote(o.getId()))
            .append(",\"amount\":").append(o.getAmount())
            .append(",\"status\":").append(JsonUtil.quote(o.getStatus().dbValue()))
            .append(",\"canRespond\":").append(o.isPending() && !userId.equals(o.getProposerId()))
            .append(",\"expiresAt\":").append(epochMs(o.getExpiresAt()))
            .append('}');
    }
}
