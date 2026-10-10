package org.example.cunguoimoita.chat.model;

import java.time.LocalDateTime;

/** Class Message trong class diagram. send() nằm trong ChatService.sendMessage(). */
public class Message {

    private String id;
    private String conversationId;
    private String senderId;
    private MessageType type = MessageType.TEXT;
    private String content;       // chữ, đường dẫn ảnh, hoặc id của PriceOffer khi type = price_offer
    private LocalDateTime sentAt;
    private boolean isRead;

    // Chỉ để hiển thị khung giá trong chat, không lưu DB
    private PriceOffer offer;

    /** + markAsRead(): void */
    public void markAsRead() {
        isRead = true;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
    public PriceOffer getOffer() { return offer; }
    public void setOffer(PriceOffer offer) { this.offer = offer; }
}
