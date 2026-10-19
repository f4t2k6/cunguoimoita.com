package org.example.cunguoimoita.chat.model;

import org.example.cunguoimoita.product.model.ProductCondition;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
    private String askerName;
    private String sellerName;
    private String askerAvatar;                // users.avatar_url (khung chat nổi hiện ảnh thay cho chữ cái đầu)
    private String sellerAvatar;
    private String productName;
    private Double productPrice;
    private String productImage;               // ảnh bìa của món (khung sản phẩm đầu đoạn chat)
    private ProductCondition productCondition;

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

    /** Tên người còn lại (từ bảng users); chưa có tên thì dùng id. */
    public String otherName(String userId) {
        String name = userId.equals(askerId) ? sellerName : askerName;
        return name == null ? otherParty(userId) : name;
    }

    /** Ảnh đại diện người còn lại, null nếu chưa có. */
    public String otherAvatar(String userId) {
        return userId.equals(askerId) ? sellerAvatar : askerAvatar;
    }

    /** Chữ cái đầu của tên cuối (Mai Thành → T), dùng làm avatar tạm. */
    public String otherInitial(String userId) {
        String[] parts = otherName(userId).trim().split("\\s+");
        String last = parts[parts.length - 1];
        return last.isEmpty() ? "?" : last.substring(0, 1).toUpperCase();
    }

    /** Giờ tin cuối cho hộp thư: hôm nay thì "14:05", ngày khác thì "12/10". */
    public String getLastTimeLabel() {
        LocalDateTime t = lastMessageAt == null ? createdAt : lastMessageAt;
        if (t == null) {
            return "";
        }
        String pattern = t.toLocalDate().equals(LocalDate.now()) ? "HH:mm" : "dd/MM";
        return t.format(DateTimeFormatter.ofPattern(pattern));
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
    public String getAskerName() { return askerName; }
    public void setAskerName(String askerName) { this.askerName = askerName; }
    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }
    public String getAskerAvatar() { return askerAvatar; }
    public void setAskerAvatar(String askerAvatar) { this.askerAvatar = askerAvatar; }
    public String getSellerAvatar() { return sellerAvatar; }
    public void setSellerAvatar(String sellerAvatar) { this.sellerAvatar = sellerAvatar; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public Double getProductPrice() { return productPrice; }
    public void setProductPrice(Double productPrice) { this.productPrice = productPrice; }
    public String getProductImage() { return productImage; }
    public void setProductImage(String productImage) { this.productImage = productImage; }
    public ProductCondition getProductCondition() { return productCondition; }
    public void setProductCondition(ProductCondition productCondition) { this.productCondition = productCondition; }
}
