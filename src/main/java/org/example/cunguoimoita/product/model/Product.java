package org.example.cunguoimoita.product.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Class Product trong class diagram: một món đồ người bán đăng lên sàn.
 * Ngoài các cột của bảng products còn mang theo vài thông tin để hiển thị
 * (tên người bán, tên danh mục, đã lưu vào Bucket List chưa).
 */
public class Product {

    private String id;
    private String sellerId;
    private String categoryId;
    private String approvedByAdminId;
    private String name;
    private String description;
    private List<String> images = new ArrayList<>();
    private ListingType listingType;
    private BigDecimal price;
    private BigDecimal estimatedValue;
    private String desiredExchangeNote;
    private ProductCondition condition;
    private boolean acceptCod = true;
    private String verifiedStatus = "unverified";
    private ProductStatus status = ProductStatus.DRAFT;
    private LocalDate postedDate;

    // Chỉ để hiển thị, đọc kèm khi JOIN bảng khác
    private String sellerName;
    private String sellerAvatar;
    private String categoryName;
    private String parentCategoryId;
    private String parentCategoryName;
    private boolean saved;

    // ---------------------------------------------------------------
    // Trạng thái nhìn từ phía người dùng (gộp status, verifiedStatus và việc admin đã duyệt chưa)
    // ---------------------------------------------------------------

    /** Admin đã duyệt bài này ít nhất một lần. */
    public boolean isApproved() {
        return approvedByAdminId != null;
    }

    public boolean isRejected() {
        return "rejected".equals(verifiedStatus);
    }

    /** Bài mới đăng (hoặc vừa sửa sau khi bị từ chối), đang đợi admin duyệt. */
    public boolean isPending() {
        return !isApproved() && !isRejected() && status == ProductStatus.DRAFT;
    }

    /** Bài đã duyệt nhưng người bán tự tạm ẩn. */
    public boolean isPaused() {
        return isApproved() && status == ProductStatus.DRAFT;
    }

    /** Đang hiện trên sàn, khách xem được: đã duyệt và đang bán hoặc đã có người đặt. */
    public boolean isListed() {
        return isApproved() && (status == ProductStatus.AVAILABLE || status == ProductStatus.RESERVED);
    }

    /** Còn mua / thêm vào giỏ được. */
    public boolean isAvailable() {
        return isApproved() && status == ProductStatus.AVAILABLE;
    }

    /** Mã trạng thái hiển thị, dùng làm class CSS: pending, rejected, paused, available, reserved, sold, exchanged, hidden. */
    public String getStateCode() {
        if (isRejected() && status == ProductStatus.DRAFT) {
            return "rejected";
        }
        if (isPending()) {
            return "pending";
        }
        if (isPaused()) {
            return "paused";
        }
        return status.getCode();
    }

    public String getStateLabel() {
        switch (getStateCode()) {
            case "rejected": return "Bị từ chối";
            case "pending":  return "Chờ duyệt";
            case "paused":   return "Đang tạm ẩn";
            default:         return status.getLabel();
        }
    }

    /** Người bán chỉ sửa được khi bài chưa có ai đặt và chưa bị admin ẩn. */
    public boolean isEditable() {
        return status == ProductStatus.DRAFT || status == ProductStatus.AVAILABLE;
    }

    public boolean isPausable() {
        return isApproved() && status == ProductStatus.AVAILABLE;
    }

    public boolean isResumable() {
        return isPaused();
    }

    // ---------------------------------------------------------------
    // Tiện ích cho giao diện
    // ---------------------------------------------------------------

    /** Ảnh bìa: ảnh đầu tiên, null nếu bài không có ảnh. */
    public String getCoverImage() {
        return images.isEmpty() ? null : images.get(0);
    }

    /** Ngày đăng kiểu "18 tháng 10" như trên mockup. */
    public String getPostedDateLabel() {
        if (postedDate == null) {
            return "";
        }
        String label = postedDate.getDayOfMonth() + " tháng " + postedDate.getMonthValue();
        return postedDate.getYear() == LocalDate.now().getYear() ? label : label + ", " + postedDate.getYear();
    }

    /** Giá dùng để so khi gợi ý trao đổi: giá trị ước tính, không có thì lấy giá bán. */
    public BigDecimal getSwapValue() {
        return estimatedValue != null ? estimatedValue : price;
    }

    // ---------------------------------------------------------------
    // Getter / setter
    // ---------------------------------------------------------------

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
    public String getApprovedByAdminId() { return approvedByAdminId; }
    public void setApprovedByAdminId(String approvedByAdminId) { this.approvedByAdminId = approvedByAdminId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images == null ? new ArrayList<>() : images; }
    public ListingType getListingType() { return listingType; }
    public void setListingType(ListingType listingType) { this.listingType = listingType; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getEstimatedValue() { return estimatedValue; }
    public void setEstimatedValue(BigDecimal estimatedValue) { this.estimatedValue = estimatedValue; }
    public String getDesiredExchangeNote() { return desiredExchangeNote; }
    public void setDesiredExchangeNote(String desiredExchangeNote) { this.desiredExchangeNote = desiredExchangeNote; }
    public ProductCondition getCondition() { return condition; }
    public void setCondition(ProductCondition condition) { this.condition = condition; }
    public boolean isAcceptCod() { return acceptCod; }
    public void setAcceptCod(boolean acceptCod) { this.acceptCod = acceptCod; }
    public String getVerifiedStatus() { return verifiedStatus; }
    public void setVerifiedStatus(String verifiedStatus) { this.verifiedStatus = verifiedStatus; }
    public ProductStatus getStatus() { return status; }
    public void setStatus(ProductStatus status) { this.status = status; }
    public LocalDate getPostedDate() { return postedDate; }
    public void setPostedDate(LocalDate postedDate) { this.postedDate = postedDate; }
    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }
    public String getSellerAvatar() { return sellerAvatar; }
    public void setSellerAvatar(String sellerAvatar) { this.sellerAvatar = sellerAvatar; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getParentCategoryId() { return parentCategoryId; }
    public void setParentCategoryId(String parentCategoryId) { this.parentCategoryId = parentCategoryId; }
    public String getParentCategoryName() { return parentCategoryName; }
    public void setParentCategoryName(String parentCategoryName) { this.parentCategoryName = parentCategoryName; }
    public boolean isSaved() { return saved; }
    public void setSaved(boolean saved) { this.saved = saved; }
}
