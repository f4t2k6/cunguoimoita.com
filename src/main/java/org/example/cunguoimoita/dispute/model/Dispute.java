package org.example.cunguoimoita.dispute.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Thực thể Dispute ánh xạ từ Class Diagram Final:
 * Dispute (id, orderId, raisedById, againstId, reason, description, evidenceImages,
 *          status, createdDate, resolvedDate, handledByAdminId, resolutionNote)
 * Thêm phần phản hồi của người bán (sellerResponse, sellerEvidenceImages, sellerResponseDate), xem 11_hoan_thien_don_hang.sql.
 */
public class Dispute implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String orderId;
    private String raisedById;
    private String againstId;
    private DisputeReason reason;
    private String description;
    private String evidenceImages;
    private DisputeStatus status;
    private LocalDate createdDate;
    private LocalDate resolvedDate;
    private String handledByAdminId;
    private String resolutionNote;
    private String sellerResponse;              // người bán giải thích
    private String sellerEvidenceImages;        // ảnh của người bán, cách nhau bởi dấu phẩy
    private LocalDateTime sellerResponseDate;

    public Dispute() {
        this.status = DisputeStatus.OPEN;
        this.createdDate = LocalDate.now();
    }

    public Dispute(String id, String orderId, String raisedById, String againstId,
                   DisputeReason reason, String description, String evidenceImages,
                   DisputeStatus status, LocalDate createdDate, LocalDate resolvedDate,
                   String handledByAdminId, String resolutionNote) {
        this.id = id;
        this.orderId = orderId;
        this.raisedById = raisedById;
        this.againstId = againstId;
        this.reason = reason;
        this.description = description;
        this.evidenceImages = evidenceImages;
        this.status = status;
        this.createdDate = createdDate;
        this.resolvedDate = resolvedDate;
        this.handledByAdminId = handledByAdminId;
        this.resolutionNote = resolutionNote;
    }

    public void submitEvidence(String newImages) {
        if (this.evidenceImages == null || this.evidenceImages.isBlank()) {
            this.evidenceImages = newImages;
        } else if (newImages != null && !newImages.isBlank()) {
            this.evidenceImages = this.evidenceImages + "," + newImages;
        }
    }

    /** Người bán gửi lời giải thích + ảnh. Gửi lại thì ghi đè lời giải thích, ảnh được cộng thêm. */
    public void respond(String response, String images) {
        requireOpen();
        if (response == null || response.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập lời giải thích");
        }
        this.sellerResponse = response.trim();
        if (images != null && !images.isBlank()) {
            this.sellerEvidenceImages = (sellerEvidenceImages == null || sellerEvidenceImages.isBlank())
                    ? images : sellerEvidenceImages + "," + images;
        }
        this.sellerResponseDate = LocalDateTime.now();
    }

    /** Còn đang xử lý (open / reviewing) không. */
    public boolean isOpen() {
        return status == DisputeStatus.OPEN || status == DisputeStatus.REVIEWING;
    }

    /** Khiếu nại đã có kết quả thì không đổi được nữa. */
    public void requireOpen() {
        if (!isOpen()) {
            throw new IllegalStateException("Khiếu nại đã được xử lý xong");
        }
    }

    /** Ảnh bằng chứng của người mua dạng danh sách (để JSP lặp). */
    public java.util.List<String> getEvidenceList() {
        return split(evidenceImages);
    }

    public java.util.List<String> getSellerEvidenceList() {
        return split(sellerEvidenceImages);
    }

    private static java.util.List<String> split(String images) {
        java.util.List<String> list = new java.util.ArrayList<>();
        if (images != null) {
            for (String part : images.split(",")) {
                if (!part.isBlank()) {
                    list.add(part.trim());
                }
            }
        }
        return list;
    }

    public void review() {
        requireOpen();
        this.status = DisputeStatus.REVIEWING;
    }

    public void resolveForBuyer(String adminId, String note) {
        requireOpen();
        this.status = DisputeStatus.RESOLVED_REFUND;
        this.handledByAdminId = adminId;
        this.resolutionNote = note;
        this.resolvedDate = LocalDate.now();
    }

    public void resolveForSeller(String adminId, String note) {
        requireOpen();
        this.status = DisputeStatus.RESOLVED_PAYOUT;
        this.handledByAdminId = adminId;
        this.resolutionNote = note;
        this.resolvedDate = LocalDate.now();
    }

    public void reject(String adminId, String note) {
        requireOpen();
        this.status = DisputeStatus.REJECTED;
        this.handledByAdminId = adminId;
        this.resolutionNote = note;
        this.resolvedDate = LocalDate.now();
    }

    // Getters and Setters
    public String getSellerResponse() {
        return sellerResponse;
    }

    public void setSellerResponse(String sellerResponse) {
        this.sellerResponse = sellerResponse;
    }

    public String getSellerEvidenceImages() {
        return sellerEvidenceImages;
    }

    public void setSellerEvidenceImages(String sellerEvidenceImages) {
        this.sellerEvidenceImages = sellerEvidenceImages;
    }

    public LocalDateTime getSellerResponseDate() {
        return sellerResponseDate;
    }

    public void setSellerResponseDate(LocalDateTime sellerResponseDate) {
        this.sellerResponseDate = sellerResponseDate;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getRaisedById() {
        return raisedById;
    }

    public void setRaisedById(String raisedById) {
        this.raisedById = raisedById;
    }

    public String getAgainstId() {
        return againstId;
    }

    public void setAgainstId(String againstId) {
        this.againstId = againstId;
    }

    public DisputeReason getReason() {
        return reason;
    }

    public void setReason(DisputeReason reason) {
        this.reason = reason;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEvidenceImages() {
        return evidenceImages;
    }

    public void setEvidenceImages(String evidenceImages) {
        this.evidenceImages = evidenceImages;
    }

    public DisputeStatus getStatus() {
        return status;
    }

    public void setStatus(DisputeStatus status) {
        this.status = status;
    }

    public LocalDate getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDate createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDate getResolvedDate() {
        return resolvedDate;
    }

    public void setResolvedDate(LocalDate resolvedDate) {
        this.resolvedDate = resolvedDate;
    }

    public String getHandledByAdminId() {
        return handledByAdminId;
    }

    public void setHandledByAdminId(String handledByAdminId) {
        this.handledByAdminId = handledByAdminId;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    public void setResolutionNote(String resolutionNote) {
        this.resolutionNote = resolutionNote;
    }
}
