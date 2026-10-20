package org.example.cunguoimoita.product.dao;

import org.example.cunguoimoita.product.model.ListingType;
import org.example.cunguoimoita.product.model.Product;
import org.example.cunguoimoita.product.model.ProductCondition;
import org.example.cunguoimoita.product.model.ProductFilter;
import org.example.cunguoimoita.product.model.ProductStatus;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Bảng products; mọi hàm nhận Connection để service gộp nhiều câu lệnh vào một transaction. */
public class ProductDAO {

    /** Cột images lưu danh sách đường dẫn ảnh, ngăn cách bằng dấu phẩy. */
    private static final String IMAGE_SEPARATOR = ",";

    /** Bài đã duyệt và đang bán: điều kiện chung cho mọi trang mà khách xem. */
    private static final String PUBLIC_CONDITION = " p.approved_by_admin_id IS NOT NULL AND p.status = 'available'";

    private static final String SELECT = "SELECT p.*, u.fullname AS seller_name, u.avatar_url AS seller_avatar,"
            + " cg.name AS category_name, cg.parent_category_id, pc.name AS parent_category_name"
            + " FROM products p"
            + " JOIN users u ON u.id = p.seller_id"
            + " JOIN categories cg ON cg.id = p.category_id"
            + " LEFT JOIN categories pc ON pc.id = cg.parent_category_id";


    /** Vài cột của bài đăng cho giỏ hàng, đơn hàng và chat. */
    public record ProductRef(String id, String sellerId, String name, ProductStatus status) {
        public boolean isAvailable() {
            return status == ProductStatus.AVAILABLE;
        }

        /** Còn hiện trên sàn để người khác hỏi mua. */
        public boolean isOpenForChat() {
            return status == ProductStatus.AVAILABLE || status == ProductStatus.RESERVED;
        }
    }

    /** Tên và ảnh bìa của món để hiện trong trang đơn hàng. */
    public record ProductCard(String id, String name, String coverImage) {
    }

    public ProductRef findRef(Connection c, String productId) throws SQLException {
        return queryRef(c, "SELECT id, seller_id, name, status FROM products WHERE id = ?", productId);
    }

    /** Khoá dòng sản phẩm tới hết transaction để 2 người không cùng chốt 1 món. */
    public ProductRef findRefForUpdate(Connection c, String productId) throws SQLException {
        return queryRef(c, "SELECT id, seller_id, name, status FROM products WHERE id = ? FOR UPDATE", productId);
    }

    public ProductCard findCard(Connection c, String productId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT id, name, images FROM products WHERE id = ?")) {
            ps.setString(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return new ProductCard(productId, productId, null);
                }
                String images = rs.getString("images");
                String cover = images == null || images.isBlank() ? null : images.split(",")[0].trim();
                return new ProductCard(rs.getString("id"), rs.getString("name"), cover);
            }
        }
    }

    public Product findById(Connection c, String id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT + " WHERE p.id = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** Khoá dòng tới hết transaction, để hai thao tác cùng lúc không ghi đè trạng thái của nhau. */
    public Product findByIdForUpdate(Connection c, String id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT + " WHERE p.id = ? FOR UPDATE OF p")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** Tất cả bài của một người bán, mới nhất trước (trang "Bài đăng của tôi"). */
    public List<Product> findBySeller(Connection c, String sellerId) throws SQLException {
        String sql = SELECT + " WHERE p.seller_id = ? ORDER BY p.posted_date DESC, p.name";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, sellerId);
            return list(ps);
        }
    }

    /** Bài đang bán của một người bán, để hiện trên hồ sơ công khai. */
    public List<Product> findPublicBySeller(Connection c, String sellerId, int limit) throws SQLException {
        String sql = SELECT + " WHERE p.seller_id = ? AND" + PUBLIC_CONDITION
                + " ORDER BY p.posted_date DESC, p.id DESC LIMIT ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, sellerId);
            ps.setInt(2, limit);
            return list(ps);
        }
    }

    /** Các bài trong Bucket List của khách hàng, kể cả bài đã bán hay đã ẩn (trang hiện rõ là không còn bán). */
    public List<Product> findSaved(Connection c, String customerId) throws SQLException {
        String sql = SELECT
                + " JOIN wishlist_products wp ON wp.product_id = p.id"
                + " JOIN wishlists w ON w.id = wp.wishlist_id"
                + " WHERE w.customer_id = ? ORDER BY (p.status = 'available') DESC, p.posted_date DESC, p.id";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, customerId);
            List<Product> list = list(ps);
            list.forEach(p -> p.setSaved(true));
            return list;
        }
    }

    /** Guest.browseProduct() / searchProduct(): một trang kết quả theo bộ lọc. */
    public List<Product> search(Connection c, ProductFilter f) throws SQLException {
        List<Object> params = new ArrayList<>();
        String sql = SELECT + " WHERE" + PUBLIC_CONDITION + where(f, params) + orderBy(f) + " LIMIT ? OFFSET ?";
        params.add(ProductFilter.PAGE_SIZE);
        params.add(f.getOffset());
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            return list(ps);
        }
    }

    /** Tổng số bài khớp bộ lọc, để tính số trang. */
    public int count(Connection c, ProductFilter f) throws SQLException {
        List<Object> params = new ArrayList<>();
        String sql = "SELECT COUNT(*) FROM products p WHERE" + PUBLIC_CONDITION + where(f, params);
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /** Product.suggestMatches(): bài nhận trao đổi có giá trị ước tính lệch không quá tolerance, gần nhất xếp trước. */
    public List<Product> findSwapMatches(Connection c, Product p, double tolerance, int limit) throws SQLException {
        BigDecimal value = p.getSwapValue();
        if (value == null) {
            return List.of();
        }
        String sql = SELECT + " WHERE" + PUBLIC_CONDITION
                + " AND p.listing_type IN ('swap', 'both') AND p.id <> ? AND p.seller_id <> ?"
                + " AND COALESCE(p.estimated_value, p.price) BETWEEN ? AND ?"
                + " ORDER BY ABS(COALESCE(p.estimated_value, p.price) - ?), p.posted_date DESC LIMIT ?";
        BigDecimal delta = value.multiply(BigDecimal.valueOf(tolerance));
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p.getId());
            ps.setString(2, p.getSellerId());
            ps.setBigDecimal(3, value.subtract(delta));
            ps.setBigDecimal(4, value.add(delta));
            ps.setBigDecimal(5, value);
            ps.setInt(6, limit);
            return list(ps);
        }
    }

    /** Có bản ghi nào khác (đơn, giỏ, cuộc trò chuyện...) đang trỏ tới bài này không. */
    public boolean isReferenced(Connection c, String productId) throws SQLException {
        String sql = "SELECT EXISTS (SELECT 1 FROM order_items WHERE product_id = ?)"
                + " OR EXISTS (SELECT 1 FROM conversations WHERE product_id = ?)"
                + " OR EXISTS (SELECT 1 FROM cart_items WHERE product_id = ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, productId);
            ps.setString(2, productId);
            ps.setString(3, productId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBoolean(1);
            }
        }
    }


    /** Customer.postProduct(): bài mới luôn ở trạng thái draft, chưa duyệt. */
    public void insert(Connection c, Product p) throws SQLException {
        String sql = "INSERT INTO products (id, seller_id, category_id, name, description, images, listing_type,"
                + " price, estimated_value, desired_exchange_note, condition, accept_cod, verified_status, status,"
                + " posted_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'unverified', 'draft', CURRENT_DATE)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p.getId());
            ps.setString(2, p.getSellerId());
            bindEditable(ps, p, 3);
            ps.executeUpdate();
        }
    }

    /** Lưu nội dung người bán vừa sửa (không đổi người bán, trạng thái duyệt hay ngày đăng). */
    public void update(Connection c, Product p) throws SQLException {
        String sql = "UPDATE products SET category_id = ?, name = ?, description = ?, images = ?, listing_type = ?,"
                + " price = ?, estimated_value = ?, desired_exchange_note = ?, condition = ?, accept_cod = ?"
                + " WHERE id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            int next = bindEditable(ps, p, 1);
            ps.setString(next, p.getId());
            ps.executeUpdate();
        }
    }

    /** Product.changeStatus(): reserve(), markSold(), người bán ẩn / hiện lại bài... */
    public void updateStatus(Connection c, String productId, ProductStatus status) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE products SET status = ? WHERE id = ?")) {
            ps.setString(1, status.getCode());
            ps.setString(2, productId);
            ps.executeUpdate();
        }
    }

    /** Bài bị từ chối được sửa lại thì đưa về hàng chờ duyệt. */
    public void resubmit(Connection c, String productId) throws SQLException {
        String sql = "UPDATE products SET verified_status = 'unverified', status = 'draft' WHERE id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, productId);
            ps.executeUpdate();
        }
    }

    /** Xoá hẳn bài (Wishlist tự bỏ bài này nhờ ON DELETE CASCADE). */
    public void delete(Connection c, String productId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM products WHERE id = ?")) {
            ps.setString(1, productId);
            ps.executeUpdate();
        }
    }


    /** Gán các cột người bán nhập trong form, bắt đầu từ vị trí start; trả về vị trí dấu ? tiếp theo. */
    private int bindEditable(PreparedStatement ps, Product p, int start) throws SQLException {
        int i = start;
        ps.setString(i++, p.getCategoryId());
        ps.setString(i++, p.getName());
        ps.setString(i++, p.getDescription());
        ps.setString(i++, p.getImages().isEmpty() ? null : String.join(IMAGE_SEPARATOR, p.getImages()));
        ps.setString(i++, p.getListingType().getCode());
        setDecimal(ps, i++, p.getPrice());
        setDecimal(ps, i++, p.getEstimatedValue());
        ps.setString(i++, p.getDesiredExchangeNote());
        ps.setString(i++, p.getCondition().getCode());
        ps.setBoolean(i++, p.isAcceptCod());
        return i;
    }

    /** Ghép phần WHERE theo bộ lọc; giá trị đưa vào params theo đúng thứ tự dấu ?. */
    private String where(ProductFilter f, List<Object> params) {
        StringBuilder sql = new StringBuilder();
        if (f.getKeyword() != null) {
            sql.append(" AND (p.name ILIKE ? OR p.description ILIKE ?)");
            String like = "%" + escapeLike(f.getKeyword()) + "%";
            params.add(like);
            params.add(like);
        }
        if (f.getCategoryId() != null) {
            // Chọn danh mục cha thì ra cả bài thuộc các danh mục con của nó
            sql.append(" AND (p.category_id = ? OR p.category_id IN"
                    + " (SELECT id FROM categories WHERE parent_category_id = ?))");
            params.add(f.getCategoryId());
            params.add(f.getCategoryId());
        }
        if (f.getMinPrice() != null) {
            sql.append(" AND COALESCE(p.price, p.estimated_value) >= ?");
            params.add(f.getMinPrice());
        }
        if (f.getMaxPrice() != null) {
            sql.append(" AND COALESCE(p.price, p.estimated_value) <= ?");
            params.add(f.getMaxPrice());
        }
        if (f.getCondition() != null) {
            sql.append(" AND p.condition = ?");
            params.add(f.getCondition().getCode());
        }
        if (f.getListingType() != null) {
            // Lọc "Bán" thì ra cả bài "Bán hoặc đổi", lọc "Trao đổi" cũng vậy
            sql.append(" AND p.listing_type IN (?, 'both')");
            params.add(f.getListingType().getCode());
        }
        return sql.toString();
    }

    private String orderBy(ProductFilter f) {
        switch (f.getSort()) {
            case PRICE_ASC:  return " ORDER BY COALESCE(p.price, p.estimated_value) ASC NULLS LAST, p.id";
            case PRICE_DESC: return " ORDER BY COALESCE(p.price, p.estimated_value) DESC NULLS LAST, p.id";
            default:         return " ORDER BY p.posted_date DESC, p.id DESC";
        }
    }

    /** Dấu % và _ người dùng gõ vào ô tìm kiếm được hiểu là ký tự thường, không phải ký tự đại diện. */
    private String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private void bind(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }

    private void setDecimal(PreparedStatement ps, int index, BigDecimal value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.NUMERIC);
        } else {
            ps.setBigDecimal(index, value);
        }
    }

    private List<Product> list(PreparedStatement ps) throws SQLException {
        List<Product> list = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    private Product map(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setId(rs.getString("id"));
        p.setSellerId(rs.getString("seller_id"));
        p.setCategoryId(rs.getString("category_id"));
        p.setApprovedByAdminId(rs.getString("approved_by_admin_id"));
        p.setName(rs.getString("name"));
        p.setDescription(rs.getString("description"));
        String images = rs.getString("images");
        if (images != null && !images.isBlank()) {
            p.setImages(new ArrayList<>(Arrays.asList(images.split(IMAGE_SEPARATOR))));
        }
        p.setListingType(ListingType.fromCode(rs.getString("listing_type")));
        p.setPrice(rs.getBigDecimal("price"));
        p.setEstimatedValue(rs.getBigDecimal("estimated_value"));
        p.setDesiredExchangeNote(rs.getString("desired_exchange_note"));
        p.setCondition(ProductCondition.fromCode(rs.getString("condition")));
        p.setAcceptCod(rs.getBoolean("accept_cod"));
        p.setVerifiedStatus(rs.getString("verified_status"));
        p.setStatus(ProductStatus.fromCode(rs.getString("status")));
        Date posted = rs.getDate("posted_date");
        p.setPostedDate(posted == null ? null : posted.toLocalDate());
        p.setSellerName(rs.getString("seller_name"));
        p.setSellerAvatar(rs.getString("seller_avatar"));
        p.setCategoryName(rs.getString("category_name"));
        p.setParentCategoryId(rs.getString("parent_category_id"));
        p.setParentCategoryName(rs.getString("parent_category_name"));
        return p;
    }

    private ProductRef queryRef(Connection c, String sql, String productId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new ProductRef(rs.getString("id"), rs.getString("seller_id"),
                        rs.getString("name"), ProductStatus.fromCode(rs.getString("status")));
            }
        }
    }
}
