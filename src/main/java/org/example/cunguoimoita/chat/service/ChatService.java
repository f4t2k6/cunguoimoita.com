package org.example.cunguoimoita.chat.service;

import org.example.cunguoimoita.chat.dao.ConversationDAO;
import org.example.cunguoimoita.chat.dao.MessageDAO;
import org.example.cunguoimoita.chat.dao.PriceOfferDAO;
import org.example.cunguoimoita.chat.model.Conversation;
import org.example.cunguoimoita.chat.model.Message;
import org.example.cunguoimoita.chat.model.MessageType;
import org.example.cunguoimoita.chat.model.PriceOffer;
import org.example.cunguoimoita.common.db.ConnectionPool;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Nghiệp vụ F7 phần tin nhắn và trả giá.
 * Các hàm start/sendMessage/makeOffer/markAllRead/close tương ứng method của Conversation trong class diagram;
 * acceptOffer/declineOffer/counterOffer tương ứng accept/decline/counter của PriceOffer.
 */
public class ChatService {

    private final ConversationDAO conversationDAO = new ConversationDAO();
    private final MessageDAO messageDAO = new MessageDAO();
    private final PriceOfferDAO offerDAO = new PriceOfferDAO();

    /** + start(): tạo hoặc mở lại cuộc trò chuyện giữa người hỏi và người bán cho một bài. */
    public Conversation start(String productId, String askerId, String sellerId) throws SQLException {
        if (askerId.equals(sellerId)) {
            throw new IllegalArgumentException("Bạn không thể nhắn tin với chính mình");
        }
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
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

    /**
     * Tin nhắn (kèm khung giá cho tin price_offer), đồng thời markAllRead cho người đang xem.
     * after = null để lấy hết, hoặc chỉ lấy tin mới hơn mốc này (dùng cho JS hỏi tin mới).
     */
    public List<Message> loadMessages(String conversationId, String userId, Timestamp after) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            requireParticipant(c, conversationId, userId);
            offerDAO.expireDue(c);
            List<Message> messages = messageDAO.findByConversation(c, conversationId, after);
            Map<String, PriceOffer> offers = offerDAO.findByConversation(c, conversationId).stream()
                    .collect(Collectors.toMap(PriceOffer::getId, Function.identity()));
            for (Message m : messages) {
                if (m.getType() == MessageType.PRICE_OFFER) {
                    m.setOffer(offers.get(m.getContent()));
                }
            }
            messageDAO.markAllRead(c, conversationId, userId);
            return messages;
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

    /** + makeOffer(amount): PriceOffer. Mỗi cuộc trò chuyện chỉ có 1 offer pending. */
    public PriceOffer makeOffer(String conversationId, String proposerId, double amount) throws SQLException {
        return inTransaction(c -> {
            Conversation conv = requireParticipant(c, conversationId, proposerId);
            requireOpen(conv);
            offerDAO.expireDue(c);
            boolean hasPending = offerDAO.findByConversation(c, conversationId).stream().anyMatch(PriceOffer::isPending);
            if (hasPending) {
                throw new IllegalStateException("Đang có một offer chờ trả lời, hãy chấp nhận, từ chối hoặc trả giá lại offer đó");
            }
            PriceOffer offer = PriceOffer.create(conversationId, proposerId, amount);
            saveOffer(c, offer);
            return offer;
        });
    }

    /**
     * PriceOffer.accept(): offer chuyển accepted, các offer pending khác của cùng Product chuyển declined.
     * @return offer đã accept (sau này kèm Order khi ghép với F4)
     */
    public PriceOffer acceptOffer(String offerId, String userId) throws SQLException {
        PriceOffer accepted = inTransaction(c -> {
            PriceOffer offer = requireOffer(c, offerId);
            Conversation conv = requireParticipant(c, offer.getConversationId(), userId);
            requireOpen(conv);
            offer.accept(userId);
            offerDAO.updateStatus(c, offer);
            for (PriceOffer other : offerDAO.findPendingOfSameProduct(c, conv.getProductId(), offer.getId())) {
                other.autoDecline();
                offerDAO.updateStatus(c, other);
            }
            insertMessage(c, conv.getId(), userId, MessageType.TEXT, "Đã chấp nhận giá " + money(offer.getAmount()));
            return offer;
        });
        // TODO F2 (TV2): ProductService.reserve(productId) để món chuyển reserved
        // TODO F4 (TV3): Order order = OrderService.createFromOffer(offer) rồi đưa người mua sang trang thanh toán
        return accepted;
    }

    /** PriceOffer.decline() */
    public void declineOffer(String offerId, String userId) throws SQLException {
        inTransaction(c -> {
            PriceOffer offer = requireOffer(c, offerId);
            Conversation conv = requireParticipant(c, offer.getConversationId(), userId);
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
            PriceOffer counter = offer.counter(userId, amount);
            offerDAO.updateStatus(c, offer);
            saveOffer(c, counter);
            return counter;
        });
    }

    /** + close(): khi món đã bán (F2/F4 gọi). */
    public void close(String conversationId) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            Conversation conv = conversationDAO.findById(c, conversationId);
            if (conv != null) {
                conv.close();
                conversationDAO.updateStatus(c, conv);
            }
        }
    }

    // ---------------------------------------------------------------------

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
        m.setSentAt(LocalDateTime.now());
        messageDAO.insert(c, m);
        conversationDAO.touch(c, conversationId, Timestamp.valueOf(m.getSentAt()));
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
            }
        }
    }
}
