package org.example.cunguoimoita.common.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** Hash mật khẩu bằng PBKDF2 của JDK, không lưu mật khẩu gốc. */
public final class PasswordUtil {
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 310_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Độ dài cho phép của mật khẩu. */
    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 128;

    private PasswordUtil() {
    }

    /**
     * 8–128 ký tự, có ít nhất 1 chữ cái và 1 chữ số, không có khoảng trắng (password.js kiểm tra giống hệt).
     * @return câu báo lỗi, hoặc null nếu hợp lệ
     */
    public static String checkStrength(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            return "Mật khẩu cần có ít nhất " + MIN_LENGTH + " ký tự.";
        }
        if (password.length() > MAX_LENGTH) {
            return "Mật khẩu tối đa " + MAX_LENGTH + " ký tự.";
        }
        if (password.chars().anyMatch(Character::isWhitespace)) {
            return "Mật khẩu không được chứa khoảng trắng.";
        }
        if (password.chars().noneMatch(Character::isLetter)) {
            return "Mật khẩu cần có ít nhất 1 chữ cái.";
        }
        if (password.chars().noneMatch(Character::isDigit)) {
            return "Mật khẩu cần có ít nhất 1 chữ số.";
        }
        return null;
    }

    public static String hashPassword(String password) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] hash = derive(password.toCharArray(), salt, ITERATIONS);
        return "pbkdf2-sha256$" + ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    public static boolean verifyPassword(String password, String encodedHash) {
        if (password == null || encodedHash == null) {
            return false;
        }
        try {
            String[] parts = encodedHash.split("\\$", -1);
            if (parts.length != 4 || !"pbkdf2-sha256".equals(parts[0])) {
                return false;
            }
            int iterations = Integer.parseInt(parts[1]);
            if (iterations < 100_000 || iterations > 1_000_000) {
                return false;
            }
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = derive(password.toCharArray(), salt, iterations);
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] derive(char[] password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("Không thể tạo hash mật khẩu", e);
        } finally {
            spec.clearPassword();
            java.util.Arrays.fill(password, '\0');
        }
    }
}
