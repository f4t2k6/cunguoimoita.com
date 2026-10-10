package org.example.cunguoimoita.chat.model;

import java.time.LocalDateTime;

/**
 * Class Conversation trong class diagram.
 * start(), sendMessage(), makeOffer(), markAllRead() cần đọc/ghi DB nên nằm trong ChatService cùng tên.
 */
public class Conversation {

    private String id;
    private String productId;
    private String askerId;       // người mua quan tâm, đang hỏi
    private String sellerId;      // người đăng tin
    private ConversationStatus status = ConversationStatus.OPEN;
    private LocalDateTime createdAt;
    private LocalDateTime lastMessageAt;

    // Chỉ để hiển thị ở hộp thư, không lưu DB
    private String lastMessagePreview;
    private int unreadCount;

    /** + close(): void  (khi món đã bán) */
    public void close() {
        status = ConversationStatus.CLOSED;
    }

    public boolean isParticipant(String userId) {
        return userId != null && (userId.equals(askerId) || userId.equals(sellerId));
    }

    /** Người còn lại trong cuộc trò chuyện. */
    public String otherParty(String userId) {
        return userId.equals(askerId) ? sellerId : askerId;
    }

    public boolean isOpen() {
        return status == ConversationStatus.OPEN;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getAskerId() { return askerId; }
    public void setAskerId(String askerId) { this.askerId = askerId; }
    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    public ConversationStatus getStatus() { return status; }
    public void setStatus(ConversationStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(LocalDateTime lastMessageAt) { this.lastMessageAt = lastMessageAt; }
    public String getLastMessagePreview() { return lastMessagePreview; }
    public void setLastMessagePreview(String lastMessagePreview) { this.lastMessagePreview = lastMessagePreview; }
    public int getUnreadCount() { return unreadCount; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }
}
