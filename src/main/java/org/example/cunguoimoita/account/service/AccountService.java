package org.example.cunguoimoita.account.service;

import jakarta.servlet.http.Part;
import org.example.cunguoimoita.account.dao.AddressDAO;
import org.example.cunguoimoita.account.dao.PublicSellerProfileDAO;
import org.example.cunguoimoita.account.dao.UserDAO;
import org.example.cunguoimoita.account.model.Address;
import org.example.cunguoimoita.account.model.Customer;
import org.example.cunguoimoita.account.model.PublicSellerProfile;
import org.example.cunguoimoita.account.model.User;
import org.example.cunguoimoita.common.util.MediaStore;
import org.example.cunguoimoita.common.util.PasswordUtil;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

/** Tài khoản: đăng nhập bằng mật khẩu, hồ sơ, ảnh đại diện, đổi mật khẩu và sổ địa chỉ. */
public class AccountService {
    public static final int AVATAR_MAX_SIZE = 512;

    private final UserDAO userDAO = new UserDAO();
    private final AddressDAO addressDAO = new AddressDAO();
    private final PublicSellerProfileDAO publicProfileDAO = new PublicSellerProfileDAO();
    private final MediaStore mediaStore = new MediaStore();

    public enum PasswordChange { CHANGED, WEAK, MISMATCH, INCORRECT }

    /** @return người dùng nếu email và mật khẩu khớp, null nếu sai */
    public User authenticate(String email, String password) throws SQLException {
        User user = userDAO.findByEmail(normalizeEmail(email));
        return user != null && PasswordUtil.verifyPassword(password, user.getPasswordHash()) ? user : null;
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public PasswordChange changePassword(String userId, String oldPassword, String newPassword, String confirm)
            throws SQLException {
        if (PasswordUtil.checkStrength(newPassword) != null) {
            return PasswordChange.WEAK;
        }
        if (!newPassword.equals(confirm)) {
            return PasswordChange.MISMATCH;
        }
        User user = userDAO.findById(userId);
        if (user == null || !PasswordUtil.verifyPassword(oldPassword, user.getPasswordHash())) {
            return PasswordChange.INCORRECT;
        }
        userDAO.updatePasswordHash(user.getId(), PasswordUtil.hashPassword(newPassword));
        return PasswordChange.CHANGED;
    }

    /**
     * Lưu ảnh mới (thu nhỏ còn tối đa 512px) vào bảng media rồi bỏ ảnh cũ.
     * @return đường dẫn ảnh mới, null nếu không cập nhật được tài khoản
     * @throws IllegalArgumentException nếu file không phải ảnh hợp lệ
     */
    public String changeAvatar(String userId, Part part, String oldAvatar) throws IOException, SQLException {
        String avatarUrl = mediaStore.save(part, userId, AVATAR_MAX_SIZE);
        if (!userDAO.updateAvatarPath(userId, avatarUrl)) {
            mediaStore.delete(avatarUrl);
            return null;
        }
        mediaStore.delete(oldAvatar);   // đường dẫn ảnh kiểu cũ (trên đĩa) thì bị bỏ qua
        return avatarUrl;
    }

    public Customer findCustomer(String userId) throws SQLException {
        return userDAO.findCustomerById(userId);
    }

    public boolean updateProfile(String userId, String fullName, String phone, String payoutAccountRef)
            throws SQLException {
        return userDAO.updateCustomerProfile(userId, fullName, phone, payoutAccountRef);
    }

    public PublicSellerProfile findPublicProfile(String userId) throws SQLException {
        return publicProfileDAO.findByUserId(userId);
    }

    public boolean isActive(String userId) throws SQLException {
        return userDAO.isActive(userId);
    }

    public boolean isActiveAdmin(String userId) throws SQLException {
        return userDAO.isActiveAdmin(userId);
    }

    public List<Address> getAddresses(String userId) throws SQLException {
        return addressDAO.findByUserId(userId);
    }

    public void addAddress(String userId, String label, String addressLine, String ward, String district,
                           String province) throws SQLException {
        addressDAO.add(userId, label, addressLine, ward, district, province);
    }

    public boolean updateAddress(String id, String userId, String label, String addressLine, String ward,
                                 String district, String province) throws SQLException {
        return addressDAO.update(id, userId, label, addressLine, ward, district, province);
    }

    public boolean setDefaultAddress(String id, String userId) throws SQLException {
        return addressDAO.setDefault(id, userId);
    }

    public boolean deleteAddress(String id, String userId) throws SQLException {
        return addressDAO.delete(id, userId);
    }
}
