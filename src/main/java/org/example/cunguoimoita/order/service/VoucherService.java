package org.example.cunguoimoita.order.service;

import org.example.cunguoimoita.common.db.ConnectionPool;
import org.example.cunguoimoita.notification.model.NotificationType;
import org.example.cunguoimoita.notification.service.NotificationService;
import org.example.cunguoimoita.order.dao.VoucherDAO;
import org.example.cunguoimoita.order.model.Voucher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Admin phát voucher cho khách (F4 + F10). Mỗi voucher thuộc đúng 1 khách (vouchers.customer_id),
// nên "phát cho tất cả" = tạo cho mỗi khách 1 voucher riêng, mã = tiền tố + 4 ký tự ngẫu nhiên.
public class VoucherService {

    private final VoucherDAO voucherDAO = new VoucherDAO();
    private final NotificationService notificationService = new NotificationService();

    // Voucher mới phát gần đây cho trang admin.
    public List<VoucherDAO.VoucherRow> listRecent() throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            return voucherDAO.findRecent(c, 200);
        }
    }

    /**
     * Phát voucher.
     * @param code       mã (chữ in hoa, số, gạch ngang). Phát cho tất cả thì đây là tiền tố.
     * @param ownerEmail email khách nhận; để trống = phát cho mọi khách đang hoạt động
     * @return số voucher đã tạo
     */
    public int issue(String code, String ownerEmail, double discountValue, double minOrderValue, LocalDate expiryDate)
            throws SQLException {
        String prefix = code == null ? "" : code.trim().toUpperCase();
        if (!prefix.matches("[A-Z0-9-]{3,30}")) {
            throw new IllegalArgumentException("Mã voucher gồm 3–30 chữ in hoa, số hoặc dấu gạch ngang");
        }
        if (!(discountValue > 0) || minOrderValue < 0 || !Double.isFinite(minOrderValue)) {
            throw new IllegalArgumentException("Số tiền giảm phải lớn hơn 0 và giá trị đơn tối thiểu không âm");
        }
        if (expiryDate != null && expiryDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Ngày hết hạn đã qua");
        }
        boolean toAll = ownerEmail == null || ownerEmail.isBlank();

        List<Voucher> created = new ArrayList<>();
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            c.setAutoCommit(false);
            try {
                List<String> owners = toAll ? activeCustomers(c) : List.of(requireCustomer(c, ownerEmail.trim()));
                for (String owner : owners) {
                    String voucherCode = toAll ? prefix + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase() : prefix;
                    if (voucherDAO.codeExists(c, voucherCode)) {
                        throw new IllegalArgumentException("Mã " + voucherCode + " đã tồn tại, hãy chọn mã khác");
                    }
                    Voucher v = new Voucher();
                    v.setCode(voucherCode);
                    v.setCustomerId(owner);
                    v.setDiscountValue(discountValue);
                    v.setMinOrderValue(minOrderValue);
                    v.setExpiryDate(expiryDate);
                    voucherDAO.insert(c, v);
                    created.add(v);
                }
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
        for (Voucher v : created) {
            try {
                notificationService.send(v.getCustomerId(), NotificationType.ORDER,
                        "Bạn nhận được voucher " + v.getCode() + ": giảm " + String.format("%,.0f", v.getDiscountValue())
                                + " đ cho đơn từ " + String.format("%,.0f", v.getMinOrderValue()) + " đ"
                                + (v.getExpiryDate() == null ? "" : ", hạn " + v.getExpiryDate()) + ".",
                        "/order/list?as=buyer");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return created.size();
    }

    private List<String> activeCustomers(Connection c) throws SQLException {
        String sql = "SELECT cu.user_id FROM customers cu JOIN users u ON u.id = cu.user_id WHERE u.status = 'active'";
        List<String> ids = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ids.add(rs.getString(1));
            }
        }
        return ids;
    }

    private String requireCustomer(Connection c, String email) throws SQLException {
        String sql = "SELECT cu.user_id FROM customers cu JOIN users u ON u.id = cu.user_id WHERE LOWER(u.email) = LOWER(?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("Không có khách hàng nào dùng email " + email);
                }
                return rs.getString(1);
            }
        }
    }
}
