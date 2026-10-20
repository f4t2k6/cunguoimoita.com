package org.example.cunguoimoita.product.service;

import jakarta.servlet.http.Part;
import org.example.cunguoimoita.admin.dao.CategoryDAO;
import org.example.cunguoimoita.admin.model.Category;
import org.example.cunguoimoita.common.db.ConnectionPool;
import org.example.cunguoimoita.product.dao.ProductDAO;
import org.example.cunguoimoita.product.model.Product;
import org.example.cunguoimoita.product.model.ProductStatus;
import org.example.cunguoimoita.common.util.MediaStore;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Lỗi nhập liệu ném IllegalArgumentException, sai trạng thái ném IllegalStateException; servlet bắt để hiện thông báo. */
public class ProductService {

    public static final int MAX_IMAGES = 8;
    private static final BigDecimal MAX_MONEY = new BigDecimal("9999999999");
    private static final double SWAP_TOLERANCE = 0.3;                             // gợi ý món lệch giá trị tối đa 30%

    private final ProductDAO productDAO = new ProductDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final MediaStore imageStore = new MediaStore();


    /** Danh mục cha trước, con ngay sau cha (dùng cho ô chọn danh mục và thanh danh mục). */
    public List<Category> categories() throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            return categoryDAO.findAll(c);
        }
    }

    /** Bài đăng của tôi. */
    public List<Product> mine(String sellerId) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            return productDAO.findBySeller(c, sellerId);
        }
    }

    /** Bài đang bán của một người, cho trang hồ sơ công khai. */
    public List<Product> publicOfSeller(String sellerId, int limit) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            return productDAO.findPublicBySeller(c, sellerId, limit);
        }
    }

    /** Khách chỉ xem bài đã duyệt; người bán và admin xem được cả bài chờ duyệt / đang ẩn. @return null nếu không được xem */
    public Product detail(String productId, String viewerId, boolean viewerIsAdmin) throws SQLException {
        if (isBlank(productId)) {
            return null;
        }
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            Product p = productDAO.findById(c, productId);
            if (p == null) {
                return null;
            }
            boolean owner = p.getSellerId().equals(viewerId);
            return p.isListed() || owner || viewerIsAdmin ? p : null;
        }
    }

    /** Bài để mở form sửa: phải là bài của chính người bán. */
    public Product forEdit(String sellerId, String productId) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            return requireOwned(productDAO.findById(c, productId), sellerId);
        }
    }

    /** Product.suggestMatches(): bài trao đổi khác có giá trị gần bằng. */
    public List<Product> suggestMatches(Product p, int limit) throws SQLException {
        if (p == null || p.getListingType() == null || !p.getListingType().isForSwap()) {
            return List.of();
        }
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            return productDAO.findSwapMatches(c, p, SWAP_TOLERANCE, limit);
        }
    }


    /** Customer.postProduct(): lưu ảnh rồi tạo bài ở trạng thái chờ duyệt. */
    public String post(String sellerId, Product input, List<Part> photos) throws SQLException, IOException {
        validate(input);
        List<Part> uploads = nonEmpty(photos);
        if (uploads.isEmpty()) {
            throw new IllegalArgumentException("Cần ít nhất 1 ảnh của món đồ");
        }
        if (uploads.size() > MAX_IMAGES) {
            throw new IllegalArgumentException("Tối đa " + MAX_IMAGES + " ảnh cho một bài");
        }
        List<String> saved = saveAll(uploads, sellerId);
        input.setId(UUID.randomUUID().toString());
        input.setSellerId(sellerId);
        input.setImages(saved);
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            requireCategory(c, input.getCategoryId());
            productDAO.insert(c, input);
        } catch (SQLException | RuntimeException e) {
            saved.forEach(imageStore::delete);   // không để lại ảnh mồ côi khi lưu bài thất bại
            throw e;
        }
        return input.getId();
    }

    /** Bài đang bị từ chối thì sửa xong tự gửi duyệt lại. */
    public void update(String sellerId, String productId, Product input, List<String> keepImages, List<Part> photos)
            throws SQLException, IOException {
        validate(input);
        List<Part> uploads = nonEmpty(photos);
        List<String> newImages = List.of();
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            c.setAutoCommit(false);
            try {
                Product current = requireOwned(productDAO.findByIdForUpdate(c, productId), sellerId);
                if (!current.isEditable()) {
                    throw new IllegalStateException("Bài đã có người đặt hoặc đã bị admin ẩn nên không sửa được");
                }
                requireCategory(c, input.getCategoryId());

                // Chỉ giữ những ảnh thật sự thuộc bài này, tránh form gửi đường dẫn lạ
                List<String> kept = new ArrayList<>();
                if (keepImages != null) {
                    for (String url : keepImages) {
                        if (current.getImages().contains(url) && !kept.contains(url)) {
                            kept.add(url);
                        }
                    }
                }
                if (kept.size() + uploads.size() == 0) {
                    throw new IllegalArgumentException("Bài cần ít nhất 1 ảnh");
                }
                if (kept.size() + uploads.size() > MAX_IMAGES) {
                    throw new IllegalArgumentException("Tối đa " + MAX_IMAGES + " ảnh cho một bài");
                }
                newImages = saveAll(uploads, sellerId);
                List<String> images = new ArrayList<>(kept);
                images.addAll(newImages);

                input.setId(productId);
                input.setImages(images);
                productDAO.update(c, input);
                if (current.isRejected()) {
                    productDAO.resubmit(c, productId);
                }
                c.commit();

                // Ảnh người bán bỏ đi thì xoá file sau khi đã lưu xong
                for (String old : current.getImages()) {
                    if (!kept.contains(old)) {
                        imageStore.delete(old);
                    }
                }
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                newImages.forEach(imageStore::delete);
                throw e;
            }
        }
    }


    /** Người bán tạm ẩn bài đang bán: bài về draft nhưng vẫn giữ trạng thái đã duyệt. */
    public void pause(String sellerId, String productId) throws SQLException {
        changeOwnStatus(sellerId, productId, true);
    }

    /** Hiện lại bài đã tạm ẩn, không cần duyệt lại. */
    public void resume(String sellerId, String productId) throws SQLException {
        changeOwnStatus(sellerId, productId, false);
    }

    /** Xoá bài. Bài đã có đơn, giỏ hàng hay cuộc trò chuyện thì chỉ được ẩn để giữ lịch sử giao dịch. */
    public void delete(String sellerId, String productId) throws SQLException {
        Product removed;
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            c.setAutoCommit(false);
            try {
                removed = requireOwned(productDAO.findByIdForUpdate(c, productId), sellerId);
                if (productDAO.isReferenced(c, productId)) {
                    throw new IllegalStateException("Bài đã có người nhắn tin, cho vào giỏ hoặc đặt mua nên không xoá được, bạn có thể ẩn bài");
                }
                productDAO.delete(c, productId);
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
        removed.getImages().forEach(imageStore::delete);
    }

    private void changeOwnStatus(String sellerId, String productId, boolean pause) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            c.setAutoCommit(false);
            try {
                Product p = requireOwned(productDAO.findByIdForUpdate(c, productId), sellerId);
                if (pause && !p.isPausable()) {
                    throw new IllegalStateException("Chỉ ẩn được bài đang bán");
                }
                if (!pause && !p.isResumable()) {
                    throw new IllegalStateException("Chỉ hiện lại được bài đã duyệt mà bạn đang tạm ẩn");
                }
                productDAO.updateStatus(c, productId, pause ? ProductStatus.DRAFT : ProductStatus.AVAILABLE);
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    // Cho đặt hàng và chốt giá trong chat gọi
    // Nhận Connection để chạy chung transaction với nghiệp vụ gọi tới.


    private void validate(Product p) {
        p.setName(trimToNull(p.getName()));
        p.setDescription(trimToNull(p.getDescription()));
        p.setDesiredExchangeNote(trimToNull(p.getDesiredExchangeNote()));
        if (p.getName() == null || p.getName().length() < 3) {
            throw new IllegalArgumentException("Tên món đồ cần ít nhất 3 ký tự");
        }
        if (p.getName().length() > 200) {
            throw new IllegalArgumentException("Tên món đồ tối đa 200 ký tự");
        }
        if (p.getDescription() != null && p.getDescription().length() > 5000) {
            throw new IllegalArgumentException("Mô tả tối đa 5000 ký tự");
        }
        if (isBlank(p.getCategoryId())) {
            throw new IllegalArgumentException("Hãy chọn danh mục");
        }
        if (p.getCondition() == null) {
            throw new IllegalArgumentException("Hãy chọn tình trạng món đồ");
        }
        if (p.getListingType() == null) {
            throw new IllegalArgumentException("Hãy chọn loại bài: bán, trao đổi hoặc cả hai");
        }
        if (p.getListingType().isForSale()) {
            requireMoney(p.getPrice(), "Hãy nhập giá bán");
        } else {
            p.setPrice(null);
            p.setAcceptCod(false);     // chỉ trao đổi thì không có tiền để thu hộ
        }
        if (p.getListingType().isForSwap()) {
            requireMoney(p.getEstimatedValue(), "Hãy nhập giá trị ước tính của món để gợi ý trao đổi");
            if (p.getDesiredExchangeNote() != null && p.getDesiredExchangeNote().length() > 500) {
                throw new IllegalArgumentException("Món muốn đổi tối đa 500 ký tự");
            }
        } else {
            p.setEstimatedValue(null);
            p.setDesiredExchangeNote(null);
        }
    }

    private void requireMoney(BigDecimal value, String messageIfMissing) {
        if (value == null) {
            throw new IllegalArgumentException(messageIfMissing);
        }
        if (value.signum() < 0 || value.compareTo(MAX_MONEY) > 0) {
            throw new IllegalArgumentException("Số tiền phải từ 0 đến 9.999.999.999 đ");
        }
    }

    private void requireCategory(Connection c, String categoryId) throws SQLException {
        if (categoryDAO.isTopLevel(c, categoryId) == null) {
            throw new IllegalArgumentException("Danh mục không tồn tại");
        }
    }

    private Product requireOwned(Product p, String sellerId) {
        if (p == null || !p.getSellerId().equals(sellerId)) {
            throw new IllegalArgumentException("Không tìm thấy bài đăng của bạn");
        }
        return p;
    }

    /** Lưu từng ảnh vào bảng media, trả về danh sách "/media/<id>"; lỗi giữa chừng thì xoá các ảnh đã lưu. */
    private List<String> saveAll(List<Part> parts, String sellerId) throws IOException, SQLException {
        List<String> saved = new ArrayList<>();
        try {
            for (Part part : parts) {
                saved.add(imageStore.save(part, sellerId, 1600));
            }
        } catch (IOException | RuntimeException | SQLException e) {
            saved.forEach(imageStore::delete);
            throw e;
        }
        return saved;
    }

    /** Bỏ các ô chọn file để trống (trình duyệt vẫn gửi một Part rỗng). */
    private List<Part> nonEmpty(List<Part> parts) {
        List<Part> list = new ArrayList<>();
        if (parts != null) {
            for (Part part : parts) {
                if (part.getSize() > 0) {
                    list.add(part);
                }
            }
        }
        return list;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String trimToNull(String s) {
        return isBlank(s) ? null : s.trim();
    }
}