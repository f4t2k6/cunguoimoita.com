package org.example.cunguoimoita.chat.service;

import org.example.cunguoimoita.chat.dao.ConversationDAO;
import org.example.cunguoimoita.chat.dao.MessageDAO;
import org.example.cunguoimoita.chat.dao.PriceOfferDAO;
import org.example.cunguoimoita.chat.model.Conversation;
import org.example.cunguoimoita.chat.model.Message;
import org.example.cunguoimoita.chat.model.MessageType;
import org.example.cunguoimoita.chat.model.OfferStatus;
import org.example.cunguoimoita.chat.model.PriceOffer;
import org.example.cunguoimoita.common.dao.OrderRefDAO;
import org.example.cunguoimoita.common.dao.OrderRefDAO.NewOrder;
import org.example.cunguoimoita.common.dao.ProductRefDAO;
import org.example.cunguoimoita.common.dao.ProductRefDAO.ProductRef;
import org.example.cunguoimoita.common.db.ConnectionPool;
import org.example.cunguoimoita.common.util.MediaStore;
import org.example.cunguoimoita.notification.model.NotificationType;
import org.example.cunguoimoita.notification.service.NotificationService;
import org.example.cunguoimoita.order.model.Order;
import org.example.cunguoimoita.order.service.OrderService;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Tin nhắn và trả giá (Conversation, PriceOffer trong class diagram). */
public class ChatService {

    private final ConversationDAO conversationDAO = new ConversationDAO();
    private final MessageDAO messageDAO = new MessageDAO();
    private final PriceOfferDAO offerDAO = new PriceOfferDAO();
    private final ProductRefDAO productDAO = new ProductRefDAO();
    private final OrderRefDAO orderDAO = new OrderRefDAO();
    private final OrderService orderService = new OrderService();
    private final NotificationService notificationService = new NotificationService();

    /** Kết quả khi chấp nhận giá: offer đã accepted và đơn hàng vừa tạo. */
    public record AcceptResult(PriceOffer offer, String orderId, String orderCode) {
    }

    /** offers để chat.js cập nhật các khung giá cũ; orders để hiện nút "Xem đơn" theo id offer. */
    public record ChatUpdate(List<Message> messages, List<PriceOffer> offers, Map<String, NewOrder> orders) {
    }

    /** + start(): tạo hoặc mở lại cuộc trò chuyện giữa người hỏi và người bán (lấy từ bài đăng) cho một bài. */
    public Conversation start(String productId, String askerId) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            ProductRef product = productDAO.find(c, productId);
            if (product == null || !product.isOpenForChat()) {
                throw new IllegalArgumentException("Bài đăng không tồn tại hoặc đã ngừng bán");
            }
            String sellerId = product.sellerId();
            if (askerId.equals(sellerId)) {
                throw new IllegalArgumentException("Bạn không thể nhắn tin với chính mình");
            }
            Conversation conv = conversationDAO.findByProductAndAsker(c, productId, askerId);
            if (conv == null) {
                conv = new Conversation();
                conv.setId(UUID.randomUUID().toString());
                conv.setProductId(productId);
                conv.setAskerId(askerId);
                conv.setSellerId(sellerId);
                conv.setCreatedAt(LocalDateTime.now());
                conversationDAO.insert(c, conv);
            }
            return conv;
        }
    }

    public List<Conversation> inbox(String userId) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            return conversationDAO.findInbox(c, userId);
        }
    }

    public int unreadCount(String userId) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            return messageDAO.countUnreadForUser(c, userId);
        }
    }

    /** Lấy cuộc trò chuyện và kiểm tra userId có trong đó. */
    public Conversation getConversation(String conversationId, String userId) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            return requireParticipant(c, conversationId, userId);
        }
    }

    /** Không có tin mới thì chỉ tốn 1 câu SELECT; chỉ đọc offer và đánh dấu đã đọc khi thật sự có tin. */
    public ChatUpdate loadMessages(String conversationId, String userId, Timestamp after) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            List<Message> messages = messageDAO.findForParticipant(c, conversationId, userId, after);
            if (messages.isEmpty()) {
                return new ChatUpdate(messages, List.of(), Map.of());
            }
            List<PriceOffer> offers = offerDAO.findByConversation(c, conversationId);
            Map<String, PriceOffer> byId = offers.stream().collect(Collectors.toMap(PriceOffer::getId, Function.identity()));
            boolean hasUnread = false;
            for (Message m : messages) {
                if (m.getType() == MessageType.PRICE_OFFER) {
                    m.setOffer(byId.get(m.getContent()));
                }
                hasUnread |= !m.isRead() && !userId.equals(m.getSenderId());
            }
            if (hasUnread) {
                messageDAO.markAllRead(c, conversationId, userId);
            }
            // Chỉ hỏi bảng orders khi đã có offer được chấp nhận
            Map<String, NewOrder> orders = offers.stream().anyMatch(o -> o.getStatus() == OfferStatus.ACCEPTED)
                    ? orderDAO.findByConversationOffers(c, conversationId)
                    : Map.of();
            return new ChatUpdate(messages, offers, orders);
        }
    }

    /** + sendMessage(content): Message */
    public Message sendMessage(String conversationId, String senderId, String content) throws SQLException {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Tin nhắn trống");
        }
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            Conversation conv = requireParticipant(c, conversationId, senderId);
            requireOpen(conv);
            return insertMessage(c, conversationId, senderId, MessageType.TEXT, content.trim());
        }
    }

    /** Ảnh đã được servlet lưu qua MediaStore; ở đây chỉ ghi tin type = image. */
    public Message sendImage(String conversationId, String senderId, String imageUrl) throws SQLException {
        // Ảnh chat giờ lưu trong database (MediaStore, "/media/<id>"); vẫn nhận đường dẫn cũ /uploads/chat/
        if (imageUrl == null || !(imageUrl.startsWith(MediaStore.URL_PREFIX) || imageUrl.startsWith("/uploads/chat/"))) {
            throw new IllegalArgumentException("Ảnh không hợp lệ");
        }
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            Conversation conv = requireParticipant(c, conversationId, senderId);
            requireOpen(conv);
            return insertMessage(c, conversationId, senderId, MessageType.IMAGE, imageUrl);
        }
    }

    /** Người gửi phải ở trong cuộc trò chuyện và cuộc trò chuyện còn mở (để chặn trước khi lưu file ảnh). */
    public void requireCanSend(String conversationId, String senderId) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            requireOpen(requireParticipant(c, conversationId, senderId));
        }
    }

    /** + makeOffer(amount): PriceOffer. Mỗi cuộc trò chuyện chỉ có 1 offer pending. */
    public PriceOffer makeOffer(String conversationId, String proposerId, double amount) throws SQLException {
        return inTransaction(c -> {
            Conversation conv = requireParticipant(c, conversationId, proposerId);
            requireOpen(conv);
            requireAvailable(c, conv);
            offerDAO.expireDue(c, conversationId);
            boolean hasPending = offerDAO.findByConversation(c, conversationId).stream().anyMatch(PriceOffer::isPending);
            if (hasPending) {
                throw new IllegalStateException("Đang có một offer chờ trả lời, hãy chấp nhận, từ chối hoặc trả giá lại offer đó");
            }
            PriceOffer offer = PriceOffer.create(conversationId, proposerId, amount);
            saveOffer(c, offer);
            return offer;
        });
    }

    /** Trong một transaction: offer accepted, các offer pending khác của cùng Product declined, Product reserved, tạo Order theo giá đã chốt. */
    public AcceptResult acceptOffer(String offerId, String userId) throws SQLException {
        Conversation[] convHolder = new Conversation[1];
        AcceptResult result = inTransaction(c -> {
            PriceOffer offer = requireOffer(c, offerId);
            Conversation conv = requireParticipant(c, offer.getConversationId(), userId);
            requireOpen(conv);
            ProductRef product = productDAO.findForUpdate(c, conv.getProductId());
            if (product == null || !product.isAvailable()) {
                throw new IllegalStateException("Món này đã được chốt với người khác hoặc đã ngừng bán");
            }
            offer.accept(userId);
            offerDAO.updateStatus(c, offer);
            for (PriceOffer other : offerDAO.findPendingOfSameProduct(c, conv.getProductId(), offer.getId())) {
                other.autoDecline();
                offerDAO.updateStatus(c, other);
            }
            productDAO.updateStatus(c, product.id(), "reserved");
            // tạo đơn qua OrderService để dùng chung luật với đơn từ giỏ hàng
            Order created = orderService.createFromOffer(c, offer.getId(), conv.getAskerId(), conv.getSellerId(),
                    product.id(), offer.getAmount());
            NewOrder order = new NewOrder(created.getId(), created.getCode());
            insertMessage(c, conv.getId(), userId, MessageType.TEXT,
                    "Đã chấp nhận giá " + money(offer.getAmount()) + ". Đơn " + order.code() + " đã được tạo.");
            convHolder[0] = conv;
            return new AcceptResult(offer, order.id(), order.code());
        });
        Conversation conv = convHolder[0];
        String other = userId.equals(conv.getAskerId()) ? conv.getSellerId() : conv.getAskerId();
        notificationService.send(other, NotificationType.ORDER,
                "Giá " + money(result.offer().getAmount()) + " đã được chấp nhận, đơn " + result.orderCode() + " đã được tạo",
                "/order/detail?id=" + result.orderId());
        return result;
    }

    /** PriceOffer.decline() */
    public void declineOffer(String offerId, String userId) throws SQLException {
        inTransaction(c -> {
            PriceOffer offer = requireOffer(c, offerId);
            Conversation conv = requireParticipant(c, offer.getConversationId(), userId);
            requireOpen(conv);
            offer.decline(userId);
            offerDAO.updateStatus(c, offer);
            insertMessage(c, conv.getId(), userId, MessageType.TEXT, "Đã từ chối giá " + money(offer.getAmount()));
            return null;
        });
    }

    /** PriceOffer.counter(amount): offer cũ chuyển declined, tạo offer mới của người trả giá lại. */
    public PriceOffer counterOffer(String offerId, String userId, double amount) throws SQLException {
        return inTransaction(c -> {
            PriceOffer offer = requireOffer(c, offerId);
            Conversation conv = requireParticipant(c, offer.getConversationId(), userId);
            requireOpen(conv);
            requireAvailable(c, conv);
            PriceOffer counter = offer.counter(userId, amount);
            offerDAO.updateStatus(c, offer);
            saveOffer(c, counter);
            return counter;
        });
    }

    /** + close(): khi món đã bán. */
    public void close(String conversationId) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            Conversation conv = conversationDAO.findById(c, conversationId);
            if (conv != null) {
                conv.close();
                conversationDAO.updateStatus(c, conv);
            }
        }
    }

    private void saveOffer(Connection c, PriceOffer offer) throws SQLException {
        offerDAO.insert(c, offer);
        insertMessage(c, offer.getConversationId(), offer.getProposerId(), MessageType.PRICE_OFFER, offer.getId());
    }

    private Message insertMessage(Connection c, String conversationId, String senderId, MessageType type, String content)
            throws SQLException {
        Message m = new Message();
        m.setId(UUID.randomUUID().toString());
        m.setConversationId(conversationId);
        m.setSenderId(senderId);
        m.setType(type);
        m.setContent(content);
        // Làm tròn tới mili giây cho khớp mốc "after" (epoch ms) mà chat.js gửi lên, tránh lấy lại tin cũ
        m.setSentAt(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));
        messageDAO.insert(c, m);   // cập nhật luôn conversations.last_message_at
        return m;
    }

    private Conversation requireParticipant(Connection c, String conversationId, String userId) throws SQLException {
        Conversation conv = conversationDAO.findById(c, conversationId);
        if (conv == null || !conv.isParticipant(userId)) {
            throw new IllegalArgumentException("Không tìm thấy cuộc trò chuyện");
        }
        return conv;
    }

    private void requireOpen(Conversation conv) {
        if (!conv.isOpen()) {
            throw new IllegalStateException("Cuộc trò chuyện đã đóng vì món đã bán");
        }
    }

    /** Chỉ trả giá được khi món còn available (chưa ai chốt). */
    private void requireAvailable(Connection c, Conversation conv) throws SQLException {
        ProductRef product = productDAO.find(c, conv.getProductId());
        if (product == null || !product.isAvailable()) {
            throw new IllegalStateException("Món này đã được chốt với người khác hoặc đã ngừng bán");
        }
    }

    private PriceOffer requireOffer(Connection c, String offerId) throws SQLException {
        PriceOffer offer = offerDAO.findByIdForUpdate(c, offerId);
        if (offer == null) {
            throw new IllegalArgumentException("Không tìm thấy offer");
        }
        return offer;
    }

    private static String money(double amount) {
        return NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(amount) + " đ";
    }

    @FunctionalInterface
    private interface TxWork<T> {
        T run(Connection c) throws SQLException;
    }

    /** Chạy trong một transaction: lỗi thì rollback toàn bộ. */
    private <T> T inTransaction(TxWork<T> work) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            c.setAutoCommit(false);
            try {
                T result = work.run(c);
                c.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);   // trả kết nối về pool ở trạng thái mặc định cho người mượn sau
            }
        }
    }
}
