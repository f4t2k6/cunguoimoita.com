package org.example.cunguoimoita.chat.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.cunguoimoita.chat.model.Conversation;
import org.example.cunguoimoita.chat.model.Message;
import org.example.cunguoimoita.chat.model.PriceOffer;
import org.example.cunguoimoita.chat.service.ChatService;
import org.example.cunguoimoita.chat.service.ChatService.ChatUpdate;
import org.example.cunguoimoita.common.dao.OrderRefDAO.NewOrder;
import org.example.cunguoimoita.common.util.CurrentUser;
import org.example.cunguoimoita.common.util.JsonUtil;
import org.example.cunguoimoita.common.util.MediaStore;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

/**
 * API JSON cho chat.js (khung chat không tải lại trang).
 *  GET  /chat/messages?id=&after=   tin nhắn mới hơn mốc after (epoch ms), after bỏ trống để lấy hết.
 *                                   Có tin mới thì kèm "offers" (mọi offer của cuộc trò chuyện) để cập nhật khung giá cũ.
 *  GET  /chat/unread                số tin chưa đọc (icon tin nhắn trên header)
 *  GET  /chat/list                  các đoạn chat, mới nhất trước (khung chat nổi góc phải dưới, chat-dock.js):
 *                                   tên/ảnh người kia, món đang trao đổi, tin cuối, số tin chưa đọc
 *  POST /chat/send                  conversationId, content
 *                                   Offer đã chấp nhận kèm orderId, orderCode của đơn tạo ra.
 *  POST /chat/offer                 action=make (conversationId, amount) | accept | decline (offerId) | counter (offerId, amount)
 *  POST /chat/image   (multipart)   conversationId, image (1 ảnh ≤ 5 MB) → tin nhắn type "image", content = đường dẫn ảnh
 * Lỗi trả {"ok":false,"error":"..."}.
 */
@WebServlet(urlPatterns = {"/chat/messages", "/chat/unread", "/chat/list", "/chat/send", "/chat/offer", "/chat/image"})
@MultipartConfig(maxFileSize = MediaStore.MAX_FILE_SIZE, maxRequestSize = MediaStore.MAX_FILE_SIZE + 64 * 1024)
public class ChatApiServlet extends HttpServlet {

    private final ChatService chatService = new ChatService();
    private final MediaStore mediaStore = new MediaStore();

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
            if ("/chat/list".equals(req.getServletPath())) {
                writeList(resp, chatService.inbox(userId), userId);
                return;
            }
            ChatUpdate update = chatService.loadMessages(req.getParameter("id"), userId, afterParam(req));
            StringBuilder json = new StringBuilder("{\"ok\":true,\"messages\":[");
            List<Message> messages = update.messages();
            for (int i = 0; i < messages.size(); i++) {
                if (i > 0) {
                    json.append(',');
                }
                appendMessage(json, messages.get(i), userId, update.orders());
            }
            json.append("],\"offers\":[");
            List<PriceOffer> offers = update.offers();
            for (int i = 0; i < offers.size(); i++) {
                if (i > 0) {
                    json.append(',');
                }
                appendOffer(json, offers.get(i), userId, update.orders());
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
            } else if ("/chat/image".equals(req.getServletPath())) {
                String conversationId = req.getParameter("conversationId");
                chatService.requireCanSend(conversationId, userId);          // kiểm tra quyền trước rồi mới lưu file
                var part = req.getPart("image");
                if (part == null || part.getSize() == 0) {
                    throw new IllegalArgumentException("Chưa chọn ảnh");
                }
                chatService.sendImage(conversationId, userId, mediaStore.save(part, userId, MediaStore.PHOTO_SIDE));
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

    private static void writeList(HttpServletResponse resp, List<Conversation> list, String userId) throws IOException {
        StringBuilder json = new StringBuilder("{\"ok\":true,\"conversations\":[");
        for (int i = 0; i < list.size(); i++) {
            Conversation c = list.get(i);
            if (i > 0) {
                json.append(',');
            }
            json.append("{\"id\":").append(JsonUtil.quote(c.getId()))
                .append(",\"otherName\":").append(JsonUtil.quote(c.otherName(userId)))
                .append(",\"otherInitial\":").append(JsonUtil.quote(c.otherInitial(userId)))
                .append(",\"otherAvatar\":").append(JsonUtil.quote(c.otherAvatar(userId)))
                .append(",\"asker\":").append(userId.equals(c.getAskerId()))
                .append(",\"open\":").append(c.isOpen())
                .append(",\"buyable\":").append(c.isProductBuyable())
                .append(",\"productId\":").append(JsonUtil.quote(c.getProductId()))
                .append(",\"productName\":").append(JsonUtil.quote(c.getProductName() == null ? c.getProductId() : c.getProductName()))
                .append(",\"productPrice\":").append(c.getProductPrice() == null ? "null" : String.valueOf(c.getProductPrice().longValue()))
                .append(",\"productImage\":").append(JsonUtil.quote(c.getProductImage()))
                .append(",\"productCondition\":").append(JsonUtil.quote(c.getProductCondition() == null ? null : c.getProductCondition().getLabel()))
                .append(",\"preview\":").append(JsonUtil.quote(c.getLastMessagePreview()))
                .append(",\"time\":").append(JsonUtil.quote(c.getLastTimeLabel()))
                .append(",\"unread\":").append(c.getUnreadCount())
                .append('}');
        }
        JsonUtil.write(resp, 200, json.append("]}").toString());
    }

    private static double amount(HttpServletRequest req) {
        try {
            // Ô nhập giá hiện dấu chấm phần nghìn (1.500.000) nên bỏ dấu chấm, dấu phẩy, khoảng trắng trước khi đọc số
            double value = Double.parseDouble(req.getParameter("amount").replaceAll("[.,\\s]", ""));
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

    private static void appendMessage(StringBuilder json, Message m, String userId, Map<String, NewOrder> orders) {
        json.append("{\"id\":").append(JsonUtil.quote(m.getId()))
            .append(",\"mine\":").append(userId.equals(m.getSenderId()))
            .append(",\"type\":").append(JsonUtil.quote(m.getType().dbValue()))
            .append(",\"sentAt\":").append(epochMs(m.getSentAt()));
        PriceOffer o = m.getOffer();
        if (o != null) {
            json.append(",\"offer\":");
            appendOffer(json, o, userId, orders);
        } else {
            json.append(",\"content\":").append(JsonUtil.quote(m.getContent()));
        }
        json.append('}');
    }

    /** Offer đã chấp nhận thì kèm orderId/orderCode của đơn vừa tạo (chat.js hiện nút "Xem đơn"). */
    private static void appendOffer(StringBuilder json, PriceOffer o, String userId, Map<String, NewOrder> orders) {
        json.append("{\"id\":").append(JsonUtil.quote(o.getId()))
            .append(",\"amount\":").append(o.getAmount())
            .append(",\"status\":").append(JsonUtil.quote(o.getStatus().dbValue()))
            .append(",\"canRespond\":").append(o.isPending() && !userId.equals(o.getProposerId()))
            .append(",\"expiresAt\":").append(epochMs(o.getExpiresAt()));
        NewOrder order = orders.get(o.getId());
        if (order != null) {
            json.append(",\"orderId\":").append(JsonUtil.quote(order.id()))
                .append(",\"orderCode\":").append(JsonUtil.quote(order.code()));
        }
        json.append('}');
    }
}
