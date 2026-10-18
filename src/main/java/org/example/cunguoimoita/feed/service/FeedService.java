package org.example.cunguoimoita.feed.service;

import org.example.cunguoimoita.common.db.ConnectionPool;
import org.example.cunguoimoita.feed.dao.WishlistDAO;
import org.example.cunguoimoita.product.dao.ProductDAO;
import org.example.cunguoimoita.product.model.Product;
import org.example.cunguoimoita.product.model.ProductFilter;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

/**
 * Bảng tin, tìm kiếm và Bucket List (F3).
 * Khách chưa đăng nhập vẫn xem và tìm được; lưu bài thì cần đăng nhập (servlet kiểm tra).
 */
public class FeedService {

    /**
     * Một trang kết quả kèm tổng số bài để vẽ phân trang.
     * Viết thành class thường (không dùng record) vì EL của Tomcat 11 chỉ đọc được tên thành phần
     * của record, không gọi được các getter tính thêm như getTotalPages().
     */
    public static final class Page {
        private final List<Product> items;
        private final int total;
        private final int page;
        private final int pageSize;

        public Page(List<Product> items, int total, int page, int pageSize) {
            this.items = items;
            this.total = total;
            this.page = page;
            this.pageSize = pageSize;
        }

        public List<Product> getItems() { return items; }
        public int getTotal() { return total; }
        public int getPage() { return page; }
        public int getTotalPages() { return Math.max(1, (total + pageSize - 1) / pageSize); }
        public boolean isHasNext() { return page < getTotalPages(); }
        public boolean isHasPrevious() { return page > 1; }
    }

    private final ProductDAO productDAO = new ProductDAO();
    private final WishlistDAO wishlistDAO = new WishlistDAO();

    /**
     * Guest.browseProduct() / searchProduct(): bài đã duyệt, đang bán, theo bộ lọc.
     * viewerId khác null thì đánh dấu những bài người xem đã lưu.
     */
    public Page search(ProductFilter filter, String viewerId) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            int total = productDAO.count(c, filter);
            List<Product> items = productDAO.search(c, filter);
            markSaved(c, items, viewerId);
            return new Page(items, total, filter.getPage(), ProductFilter.PAGE_SIZE);
        }
    }

    /** Đánh dấu "đã lưu" cho một danh sách bài bất kỳ (gợi ý trao đổi, hồ sơ người bán...). */
    public void markSaved(List<Product> items, String viewerId) throws SQLException {
        if (viewerId == null || items.isEmpty()) {
            return;
        }
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            markSaved(c, items, viewerId);
        }
    }

    /** Có trong Bucket List của người xem không. */
    public boolean isSaved(String customerId, String productId) throws SQLException {
        if (customerId == null) {
            return false;
        }
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            return wishlistDAO.savedProductIds(c, customerId).contains(productId);
        }
    }

    /** Trang Bucket List. */
    public List<Product> bucketList(String customerId) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            return productDAO.findSaved(c, customerId);
        }
    }

    /**
     * Wishlist.add() / remove().
     * @param save true để lưu, false để bỏ lưu
     */
    public void setSaved(String customerId, String productId, boolean save) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            if (save) {
                Product p = productId == null ? null : productDAO.findById(c, productId);
                if (p == null || !p.isListed()) {
                    throw new IllegalArgumentException("Bài này không còn hiển thị để lưu");
                }
                if (p.getSellerId().equals(customerId)) {
                    throw new IllegalArgumentException("Không cần lưu bài của chính bạn");
                }
                wishlistDAO.add(c, wishlistDAO.findOrCreate(c, customerId), productId);
            } else {
                String wishlistId = wishlistDAO.find(c, customerId);
                if (wishlistId != null) {
                    wishlistDAO.remove(c, wishlistId, productId);
                }
            }
        }
    }

    private void markSaved(Connection c, List<Product> items, String viewerId) throws SQLException {
        if (viewerId == null || items.isEmpty()) {
            return;
        }
        Set<String> saved = wishlistDAO.savedProductIds(c, viewerId);
        for (Product p : items) {
            p.setSaved(saved.contains(p.getId()));
        }
    }
}
