package org.example.cunguoimoita.admin.service;

import org.example.cunguoimoita.admin.dao.AdminProductDAO;
import org.example.cunguoimoita.admin.dao.AdminUserDAO;
import org.example.cunguoimoita.admin.dao.CategoryDAO;
import org.example.cunguoimoita.admin.dao.DashboardDAO;
import org.example.cunguoimoita.admin.dao.MemberTierDAO;
import org.example.cunguoimoita.admin.dao.ReportDAO;
import org.example.cunguoimoita.admin.model.AdminProductRow;
import org.example.cunguoimoita.admin.model.AdminTodo;
import org.example.cunguoimoita.admin.model.AdminUserRow;
import org.example.cunguoimoita.admin.model.Category;
import org.example.cunguoimoita.admin.model.DashboardStats;
import org.example.cunguoimoita.admin.model.MemberTier;
import org.example.cunguoimoita.admin.model.OrderStatusCount;
import org.example.cunguoimoita.admin.model.Report;
import org.example.cunguoimoita.admin.model.ReportReason;
import org.example.cunguoimoita.common.db.ConnectionPool;
import org.example.cunguoimoita.notification.model.NotificationType;
import org.example.cunguoimoita.notification.service.NotificationService;
import org.example.cunguoimoita.shipping.dao.ShippingCarrierDAO;
import org.example.cunguoimoita.shipping.model.ShippingCarrier;
import org.example.cunguoimoita.shipping.service.ShippingService;
import org.example.cunguoimoita.common.util.Texts;
import org.example.cunguoimoita.product.dao.ProductDAO;
import org.example.cunguoimoita.product.model.ProductStatus;
import org.example.cunguoimoita.common.model.AccountStatus;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Nghiệp vụ trang quản trị (các method của Admin trong class diagram). Chỉ gọi từ servlet /admin/*. */
public class AdminService {

    private static final List<String> TIER_ORDER = List.of("Bronze", "Silver", "Gold", "Diamond");

    private final DashboardDAO dashboardDAO = new DashboardDAO();
    private final AdminProductDAO adminProductDAO = new AdminProductDAO();
    private final ProductDAO productDAO = new ProductDAO();
    private final AdminUserDAO userDAO = new AdminUserDAO();
    private final ReportDAO reportDAO = new ReportDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final MemberTierDAO tierDAO = new MemberTierDAO();
    private final ShippingCarrierDAO carrierDAO = new ShippingCarrierDAO();
    private final NotificationService notificationService = new NotificationService();

    /** Thông báo gửi sau khi transaction đã commit (để không báo cho người dùng một việc bị rollback). */
    private record Notice(String userId, NotificationType type, String content, String link) {
    }

    public DashboardStats stats() throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            return dashboardDAO.load(c);
        }
    }

    /** Số việc đang chờ (bài, báo cáo, khiếu nại) cho thanh menu admin. */
    public AdminTodo todo() throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            return dashboardDAO.todo(c);
        }
    }

    /** Dữ liệu thêm của trang tổng quan: đơn theo trạng thái, vài bài chờ duyệt và báo cáo mới nhất. */
    public record Overview(List<OrderStatusCount> orderStatuses, List<AdminProductRow> latestPending,
                           List<Report> latestReports) {
        public List<OrderStatusCount> getOrderStatuses() { return orderStatuses; }
        public List<AdminProductRow> getLatestPending() { return latestPending; }
        public List<Report> getLatestReports() { return latestReports; }
    }

    public Overview overview(int limit) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            return new Overview(dashboardDAO.orderStatusCounts(c),
                    adminProductDAO.find(c, "pending", null, limit),
                    reportDAO.find(c, Report.PENDING, limit));
        }
    }

    public List<AdminProductRow> products(String filter, String keyword) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            return adminProductDAO.find(c, filter == null ? "pending" : filter, keyword);
        }
    }

    /** Admin.approveProduct(): bài lên bảng tin, báo người bán. */
    public void approveProduct(String productId, String adminId) throws SQLException {
        List<Notice> notices = new ArrayList<>();
        inTransaction(c -> {
            AdminProductRow p = requireProduct(c, productId);
            if (!p.isPending()) {
                throw new IllegalStateException("Bài này không ở trạng thái chờ duyệt");
            }
            adminProductDAO.approve(c, productId, adminId);
            notices.add(new Notice(p.sellerId(), NotificationType.SYSTEM,
                    "Bài \"" + p.name() + "\" đã được duyệt và hiện trên bảng tin", "/product?id=" + productId));
            return null;
        });
        send(notices);
    }

    /** Từ chối duyệt kèm lý do (gửi cho người bán qua thông báo). */
    public void rejectProduct(String productId, String reason) throws SQLException {
        String why = requireText(reason, "Cần nhập lý do từ chối");
        List<Notice> notices = new ArrayList<>();
        inTransaction(c -> {
            AdminProductRow p = requireProduct(c, productId);
            if (!p.isPending()) {
                throw new IllegalStateException("Bài này không ở trạng thái chờ duyệt");
            }
            adminProductDAO.reject(c, productId);
            notices.add(new Notice(p.sellerId(), NotificationType.SYSTEM,
                    "Bài \"" + p.name() + "\" bị từ chối: " + why, "/product/mine?tab=rejected"));
            return null;
        });
        send(notices);
    }

    /** Ẩn bài đang hiện (vi phạm) hoặc hiện lại bài đã ẩn. */
    public void setProductHidden(String productId, boolean hidden, String reason) throws SQLException {
        List<Notice> notices = new ArrayList<>();
        inTransaction(c -> {
            AdminProductRow p = requireProduct(c, productId);
            if (hidden) {
                if (p.isHidden()) {
                    throw new IllegalStateException("Bài này đang ẩn rồi");
                }
                productDAO.updateStatus(c, productId, ProductStatus.HIDDEN);
                notices.add(new Notice(p.sellerId(), NotificationType.SYSTEM, "Bài \"" + p.name()
                        + "\" đã bị ẩn" + (Texts.isBlank(reason) ? "" : ": " + reason.trim()), null));
            } else {
                if (!p.isHidden() || p.isPending() || p.isRejected()) {
                    throw new IllegalStateException("Chỉ hiện lại được bài đã duyệt đang bị ẩn");
                }
                productDAO.updateStatus(c, productId, ProductStatus.AVAILABLE);
                notices.add(new Notice(p.sellerId(), NotificationType.SYSTEM,
                        "Bài \"" + p.name() + "\" đã được hiện lại", "/home"));
            }
            return null;
        });
        send(notices);
    }

    public List<AdminUserRow> users(String keyword, String status) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            return userDAO.search(c, keyword, status);
        }
    }

    /** Admin.banUser(): khoá tài khoản và ẩn các bài đang bán của người đó. */
    public void banUser(String userId, String adminId) throws SQLException {
        inTransaction(c -> {
            banInTx(c, userId, adminId);
            return null;
        });
    }

    public void unbanUser(String userId) throws SQLException {
        List<Notice> notices = new ArrayList<>();
        inTransaction(c -> {
            AdminUserRow u = requireUser(c, userId);
            if (!u.isBanned()) {
                throw new IllegalStateException("Tài khoản này đang hoạt động");
            }
            userDAO.updateStatus(c, userId, AccountStatus.ACTIVE);
            notices.add(new Notice(userId, NotificationType.SYSTEM,
                    "Tài khoản của bạn đã được mở khoá. Các bài bị ẩn khi khoá cần đăng lại hoặc liên hệ admin.", null));
            return null;
        });
        send(notices);
    }

    private void banInTx(Connection c, String userId, String adminId) throws SQLException {
        AdminUserRow u = requireUser(c, userId);
        if (u.isAdmin() || userId.equals(adminId)) {
            throw new IllegalStateException("Không thể khoá tài khoản admin");
        }
        if (u.isBanned()) {
            throw new IllegalStateException("Tài khoản này đã bị khoá");
        }
        userDAO.updateStatus(c, userId, AccountStatus.BANNED);
        adminProductDAO.hideAvailableOfSeller(c, userId);
    }

    public List<Report> reports(String status) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            return reportDAO.find(c, status == null ? Report.PENDING : status);
        }
    }

    /** @param targetType product | user */
    public void submitReport(String reporterId, String targetType, String targetId, String reason,
                             String description, String evidenceImages) throws SQLException {
        if (!"product".equals(targetType) && !"user".equals(targetType)) {
            throw new IllegalArgumentException("Đối tượng báo cáo không hợp lệ");
        }
        ReportReason why;
        try {
            why = ReportReason.fromDb(reason);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Chưa chọn lý do báo cáo");
        }
        String desc = description == null ? null : description.trim();
        if (desc != null && desc.length() > 2000) {
            throw new IllegalArgumentException("Mô tả tối đa 2000 ký tự");
        }
        inTransaction(c -> {
            String ownerId = targetOwner(c, targetType, targetId);
            if (ownerId == null) {
                throw new IllegalArgumentException("Không tìm thấy " + ("product".equals(targetType) ? "bài đăng" : "người dùng"));
            }
            if (ownerId.equals(reporterId)) {
                throw new IllegalArgumentException("Bạn không thể báo cáo chính mình");
            }
            if (reportDAO.hasPending(c, reporterId, targetType, targetId)) {
                throw new IllegalStateException("Bạn đã báo cáo mục này, admin đang xem xét");
            }
            Report r = new Report();
            r.setId(UUID.randomUUID().toString());
            r.setReporterId(reporterId);
            r.setTargetType(targetType);
            r.setTargetId(targetId);
            r.setReason(why);
            r.setDescription(Texts.isBlank(desc) ? null : desc);
            r.setEvidenceImages(Texts.isBlank(evidenceImages) ? null : evidenceImages);
            r.setCreatedDate(LocalDateTime.now());
            reportDAO.insert(c, r);
            return null;
        });
    }

    /** Tên của bài / người bị báo cáo để hiện trên form báo cáo. null nếu không có. */
    public String reportTargetName(String targetType, String targetId) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            if ("product".equals(targetType)) {
                AdminProductRow p = adminProductDAO.findById(c, targetId);
                return p == null ? null : p.name();
            }
            AdminUserRow u = "user".equals(targetType) ? userDAO.findById(c, targetId) : null;
            return u == null ? null : u.fullname();
        }
    }

    /** @param action none | hide_product | ban_user (khoá người bị báo cáo hoặc người bán của bài bị báo cáo) */
    public void resolveReport(String reportId, String action, String adminId) throws SQLException {
        List<Notice> notices = new ArrayList<>();
        inTransaction(c -> {
            Report r = requirePendingReport(c, reportId);
            String result;
            switch (action == null ? "" : action) {
                case "hide_product" -> {
                    if (!r.isProductTarget()) {
                        throw new IllegalArgumentException("Báo cáo này không nhắm vào bài đăng");
                    }
                    AdminProductRow p = requireProduct(c, r.getTargetId());
                    if (!p.isHidden()) {
                        productDAO.updateStatus(c, p.id(), ProductStatus.HIDDEN);
                        notices.add(new Notice(p.sellerId(), NotificationType.SYSTEM,
                                "Bài \"" + p.name() + "\" đã bị ẩn do vi phạm: " + r.getReason().getLabel(), null));
                    }
                    result = "bài đăng đã bị ẩn";
                }
                case "ban_user" -> {
                    if (r.getTargetOwnerId() == null) {
                        throw new IllegalArgumentException("Không tìm thấy người cần khoá");
                    }
                    AdminUserRow u = requireUser(c, r.getTargetOwnerId());
                    if (!u.isBanned()) {
                        banInTx(c, u.id(), adminId);
                    }
                    result = "tài khoản vi phạm đã bị khoá";
                }
                case "none" -> result = "admin đã xem xét và ghi nhận";
                default -> throw new IllegalArgumentException("Thao tác không hợp lệ");
            }
            reportDAO.close(c, reportId, Report.RESOLVED, adminId);
            notices.add(new Notice(r.getReporterId(), NotificationType.SYSTEM,
                    "Báo cáo về \"" + nameOf(r) + "\" đã được xử lý: " + result + ". Cảm ơn bạn!", null));
            return null;
        });
        send(notices);
    }

    /** Bác bỏ báo cáo (không vi phạm). */
    public void rejectReport(String reportId, String adminId) throws SQLException {
        List<Notice> notices = new ArrayList<>();
        inTransaction(c -> {
            Report r = requirePendingReport(c, reportId);
            reportDAO.close(c, reportId, Report.REJECTED, adminId);
            notices.add(new Notice(r.getReporterId(), NotificationType.SYSTEM,
                    "Báo cáo về \"" + nameOf(r) + "\" đã được xem xét: chưa thấy vi phạm.", null));
            return null;
        });
        send(notices);
    }

    public List<Category> categories() throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            return categoryDAO.findAll(c);
        }
    }

    /** Admin.addCategory(). Cây 2 cấp: danh mục con chỉ được nằm dưới danh mục gốc. */
    public void addCategory(String name, String description, String parentId) throws SQLException {
        Category cg = new Category();
        cg.setId(UUID.randomUUID().toString());
        cg.setName(requireText(name, "Cần nhập tên danh mục"));
        cg.setDescription(Texts.isBlank(description) ? null : description.trim());
        cg.setParentCategoryId(Texts.isBlank(parentId) ? null : parentId);
        inTransaction(c -> {
            checkParent(c, cg.getId(), cg.getParentCategoryId());
            categoryDAO.insert(c, cg);
            return null;
        });
    }

    public void updateCategory(String id, String name, String description, String parentId) throws SQLException {
        Category cg = new Category();
        cg.setId(id);
        cg.setName(requireText(name, "Cần nhập tên danh mục"));
        cg.setDescription(Texts.isBlank(description) ? null : description.trim());
        cg.setParentCategoryId(Texts.isBlank(parentId) ? null : parentId);
        inTransaction(c -> {
            if (categoryDAO.isTopLevel(c, id) == null) {
                throw new IllegalArgumentException("Không tìm thấy danh mục");
            }
            if (cg.getParentCategoryId() != null && categoryDAO.hasChildren(c, id)) {
                throw new IllegalStateException("Danh mục đang có danh mục con nên không chuyển thành danh mục con được");
            }
            checkParent(c, id, cg.getParentCategoryId());
            categoryDAO.update(c, cg);
            return null;
        });
    }

    private void checkParent(Connection c, String id, String parentId) throws SQLException {
        if (parentId == null) {
            return;
        }
        if (parentId.equals(id)) {
            throw new IllegalArgumentException("Danh mục không thể là cha của chính nó");
        }
        Boolean top = categoryDAO.isTopLevel(c, parentId);
        if (top == null) {
            throw new IllegalArgumentException("Không tìm thấy danh mục cha");
        }
        if (!top) {
            throw new IllegalArgumentException("Danh mục cha phải là danh mục gốc (chỉ hỗ trợ 2 cấp)");
        }
    }

    public List<ShippingCarrier> carriers() throws SQLException {
        return carrierDAO.findAll();
    }

    /** ShippingCarrier.managePartner(): id rỗng = thêm mới, ngược lại sửa. */
    public void saveCarrier(String id, String name, String hotline, String logoUrl, String baseFee,
                            String estimatedDays, boolean active) throws SQLException {
        ShippingCarrier s = new ShippingCarrier();
        boolean isNew = Texts.isBlank(id);
        s.setId(isNew ? UUID.randomUUID().toString() : id);
        s.setName(requireText(name, "Cần nhập tên đơn vị vận chuyển"));
        s.setHotline(Texts.isBlank(hotline) ? null : hotline.trim());
        s.setLogoUrl(Texts.isBlank(logoUrl) ? null : logoUrl.trim());
        s.setBaseFee(parseNumber(baseFee, 0, 10_000_000, "Phí cơ bản không hợp lệ"));
        s.setEstimatedDays((int) parseNumber(estimatedDays, 1, 60, "Số ngày giao phải từ 1 đến 60"));
        s.setActive(active);
        if (isNew) {
            carrierDAO.insert(s);
        } else if (!carrierDAO.update(s)) {
            throw new IllegalArgumentException("Không tìm thấy đơn vị vận chuyển");
        }
        ShippingService.clearCarrierCache();
    }

    public void setCarrierActive(String id, boolean active) throws SQLException {
        if (!carrierDAO.setActive(id, active)) {
            throw new IllegalArgumentException("Không tìm thấy đơn vị vận chuyển");
        }
        ShippingService.clearCarrierCache();
    }

    public List<MemberTier> tiers() throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            return tierDAO.findAll(c);
        }
    }

    /** MemberTier.manageMemberTier(). discountPercent nhập dạng phần trăm (3 = giảm 3%). */
    public void updateTier(String id, String minEcoPoints, String discountPercent, String benefitNote)
            throws SQLException {
        MemberTier t = new MemberTier();
        t.setId(id);
        t.setMinEcoPoints((int) parseNumber(minEcoPoints, 0, 1_000_000, "Ngưỡng điểm không hợp lệ"));
        t.setDiscountRate(parseNumber(discountPercent, 0, 50, "Ưu đãi phải từ 0 đến 50%") / 100.0);
        t.setBenefitNote(Texts.isBlank(benefitNote) ? null : benefitNote.trim());
        inTransaction(c -> {
            if (!tierDAO.update(c, t)) {
                throw new IllegalArgumentException("Không tìm thấy hạng thành viên");
            }
            // Thứ tự hạng cố định Bronze < Silver < Gold < Diamond: hạng cao hơn phải cần nhiều điểm hơn
            List<MemberTier> all = new ArrayList<>(tierDAO.findAll(c));
            all.sort(Comparator.comparingInt(x -> TIER_ORDER.indexOf(x.getName())));
            if (!all.isEmpty() && all.get(0).getMinEcoPoints() != 0) {
                throw new IllegalArgumentException("Hạng " + all.get(0).getName() + " (thấp nhất) phải có ngưỡng 0 điểm");
            }
            for (int i = 1; i < all.size(); i++) {
                if (all.get(i).getMinEcoPoints() <= all.get(i - 1).getMinEcoPoints()) {
                    throw new IllegalArgumentException("Ngưỡng điểm của " + all.get(i).getName()
                            + " phải lớn hơn " + all.get(i - 1).getName());
                }
            }
            return null;
        });
    }

    private AdminProductRow requireProduct(Connection c, String productId) throws SQLException {
        AdminProductRow p = productId == null ? null : adminProductDAO.findById(c, productId);
        if (p == null) {
            throw new IllegalArgumentException("Không tìm thấy bài đăng");
        }
        return p;
    }

    private AdminUserRow requireUser(Connection c, String userId) throws SQLException {
        AdminUserRow u = userId == null ? null : userDAO.findById(c, userId);
        if (u == null) {
            throw new IllegalArgumentException("Không tìm thấy người dùng");
        }
        return u;
    }

    private Report requirePendingReport(Connection c, String reportId) throws SQLException {
        Report r = reportId == null ? null : reportDAO.findByIdForUpdate(c, reportId);
        if (r == null) {
            throw new IllegalArgumentException("Không tìm thấy báo cáo");
        }
        if (!r.isPending()) {
            throw new IllegalStateException("Báo cáo này đã được xử lý");
        }
        return r;
    }

    /** Chủ của đối tượng bị báo cáo: người bán của bài, hoặc chính người dùng đó. null nếu không tồn tại. */
    private String targetOwner(Connection c, String targetType, String targetId) throws SQLException {
        if (targetId == null) {
            return null;
        }
        if ("product".equals(targetType)) {
            AdminProductRow p = adminProductDAO.findById(c, targetId);
            return p == null ? null : p.sellerId();
        }
        AdminUserRow u = userDAO.findById(c, targetId);
        return u == null || u.isAdmin() ? null : u.id();
    }

    private static String nameOf(Report r) {
        return r.getTargetName() == null ? "(đã xoá)" : r.getTargetName();
    }

    private void send(List<Notice> notices) throws SQLException {
        for (Notice n : notices) {
            notificationService.send(n.userId(), n.type(), n.content(), n.link());
        }
    }

    private static String requireText(String value, String message) {
        if (Texts.isBlank(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static double parseNumber(String value, double min, double max, String message) {
        try {
            double v = Double.parseDouble(value.trim());
            if (!Double.isFinite(v) || v < min || v > max) {
                throw new NumberFormatException();
            }
            return v;
        } catch (NumberFormatException | NullPointerException e) {
            throw new IllegalArgumentException(message);
        }
    }

    @FunctionalInterface
    private interface TxWork<T> {
        T run(Connection c) throws SQLException;
    }

    /** Chạy trong một transaction: lỗi thì rollback toàn bộ. */
    private <T> T inTransaction(TxWork<T> work) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            c.setAutoCommit(false);
            try {
                T result = work.run(c);
                c.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }
}
