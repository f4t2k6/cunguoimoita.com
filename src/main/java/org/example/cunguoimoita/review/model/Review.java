package org.example.cunguoimoita.review.model;

import java.time.LocalDateTime;

// Class Review: 1 lần chấm điểm sau giao dịch.
// Quan hệ: Order 1 → 0..2 Review (người mua và người bán mỗi người tối đa 1 đánh giá cho 1 đơn);
//          Review → 1 Customer (tác giả), Review → 1 Customer (người được đánh giá).
public class Review {

    public static final int MAX_CONTENT = 1000;

    private String id;
    private String orderId;
    private String authorId;
    private String targetUserId;    // người được đánh giá (bên còn lại của đơn)
    private int rating;             // 1 – 5 sao
    private String content;         // nhận xét, có thể để trống
    private LocalDateTime createdDate;

    // Kiểm tra dữ liệu hợp lệ trước khi lưu (ReviewDAO mới là bên ghi DB)
    public void create() {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Số sao phải từ 1 đến 5");
        }
        if (authorId == null || authorId.equals(targetUserId)) {
            throw new IllegalArgumentException("Không thể tự đánh giá chính mình");
        }
        if (content != null) {
            content = content.trim();
            if (content.length() > MAX_CONTENT) {
                throw new IllegalArgumentException("Nhận xét tối đa " + MAX_CONTENT + " ký tự");
            }
        }
        createdDate = LocalDateTime.now();
    }

    // Chỉ tác giả được xoá đánh giá của mình (ReviewService gọi rồi mới xoá trong DB)
    public void delete(String userId) {
        if (!authorId.equals(userId)) {
            throw new SecurityException("Bạn chỉ xoá được đánh giá của chính mình");
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }
    public String getTargetUserId() { return targetUserId; }
    public void setTargetUserId(String targetUserId) { this.targetUserId = targetUserId; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
}