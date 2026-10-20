package org.example.cunguoimoita.admin.model;

import java.time.LocalDate;

/** Một dòng bài đăng trên trang duyệt bài (đọc từ products + người bán + danh mục). */
public record AdminProductRow(String id, String name, String description, String sellerId, String sellerName,
                              String categoryName, String listingType, Double price, String condition,
                              String verifiedStatus, String status, String approvedByAdminId,
                              LocalDate postedDate) {

    /** Chờ duyệt: chưa admin nào duyệt và chưa bị từ chối. */
    public boolean isPending() {
        return approvedByAdminId == null && !"rejected".equals(verifiedStatus);
    }

    public boolean isRejected() {
        return "rejected".equals(verifiedStatus);
    }

    public boolean isHidden() {
        return "hidden".equals(status);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getSellerId() { return sellerId; }
    public String getSellerName() { return sellerName; }
    public String getCategoryName() { return categoryName; }
    public String getListingType() { return listingType; }
    public Double getPrice() { return price; }
    public String getCondition() { return condition; }
    public String getVerifiedStatus() { return verifiedStatus; }
    public String getStatus() { return status; }
    public LocalDate getPostedDate() { return postedDate; }

    // EL của Tomcat đọc record qua tên thành phần (vd. ${u.banned} gọi banned()), nên thêm cả dạng này
    public boolean pending() { return isPending(); }
    public boolean rejected() { return isRejected(); }
    public boolean hidden() { return isHidden(); }
}
