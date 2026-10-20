package org.example.cunguoimoita.chat.model;

import java.time.LocalDateTime;
import java.util.UUID;

/** Mỗi Conversation chỉ có 1 offer pending; accept một offer thì Product chuyển reserved và các offer pending khác bị declined. */
public class PriceOffer {

    /** Offer hết hạn sau 24 giờ nếu không ai trả lời. */
    public static final int EXPIRE_HOURS = 24;

    private String id;
    private String conversationId;
    private String proposerId;
    private double amount;
    private OfferStatus status = OfferStatus.PENDING;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    public static PriceOffer create(String conversationId, String proposerId, double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Giá trả phải lớn hơn 0");
        }
        PriceOffer o = new PriceOffer();
        o.id = UUID.randomUUID().toString();
        o.conversationId = conversationId;
        o.proposerId = proposerId;
        o.amount = amount;
        o.createdAt = LocalDateTime.now();
        o.expiresAt = o.createdAt.plusHours(EXPIRE_HOURS);
        return o;
    }

    /** Chỉ đổi trạng thái; ChatService.acceptOffer() tạo Order. */
    public void accept(String userId) {
        requireRespondable(userId);
        status = OfferStatus.ACCEPTED;
    }

    /** + decline(): void */
    public void decline(String userId) {
        requireRespondable(userId);
        status = OfferStatus.DECLINED;
    }

    /** + counter(amount): PriceOffer  — trả giá lại: offer cũ chuyển declined, tạo offer mới của người trả lời. */
    public PriceOffer counter(String userId, double newAmount) {
        decline(userId);
        return create(conversationId, userId, newAmount);
    }

    /** Hệ thống tự từ chối (khi một offer khác của cùng món đã được accept). */
    public void autoDecline() {
        if (status == OfferStatus.PENDING) {
            status = OfferStatus.DECLINED;
        }
    }

    /** Chuyển expired nếu đã quá hạn. Trả về true nếu vừa hết hạn. */
    public boolean expireIfDue() {
        if (status == OfferStatus.PENDING && LocalDateTime.now().isAfter(expiresAt)) {
            status = OfferStatus.EXPIRED;
            return true;
        }
        return false;
    }

    public boolean isPending() {
        return status == OfferStatus.PENDING;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    public String getProposerId() { return proposerId; }
    public void setProposerId(String proposerId) { this.proposerId = proposerId; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public OfferStatus getStatus() { return status; }
    public void setStatus(OfferStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    private void requireRespondable(String userId) {
        expireIfDue();
        if (status != OfferStatus.PENDING) {
            throw new IllegalStateException("Offer này đã " + status.dbValue() + ", không trả lời được nữa");
        }
        if (proposerId.equals(userId)) {
            throw new IllegalStateException("Bạn không thể tự trả lời offer của mình");
        }
    }
}
