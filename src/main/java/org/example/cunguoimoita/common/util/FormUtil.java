package org.example.cunguoimoita.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.example.cunguoimoita.common.model.UserRole;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Vài hàm đọc form dùng chung cho các servlet. */
public final class FormUtil {

    private FormUtil() {
    }

    /** Giá trị đã bỏ khoảng trắng hai đầu; ô trống trả null. */
    public static String text(HttpServletRequest req, String name) {
        String v = req.getParameter(name);
        return v == null || v.isBlank() ? null : v.trim();
    }

    /** Chấp nhận cả "1.700.000" và "1,700,000"; ô trống trả null. */
    public static BigDecimal money(HttpServletRequest req, String name, String fieldLabel) {
        String v = text(req, name);
        if (v == null) {
            return null;
        }
        String digits = v.replaceAll("[.,\\s₫đ]", "");
        if (!digits.matches("\\d{1,13}")) {
            throw new IllegalArgumentException(fieldLabel + " chỉ gồm chữ số");
        }
        return new BigDecimal(digits);
    }

    /** Số nguyên dương, sai định dạng thì lấy giá trị mặc định. */
    public static int positiveInt(HttpServletRequest req, String name, int fallback) {
        try {
            int v = Integer.parseInt(req.getParameter(name));
            return v > 0 ? v : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** Người đang đăng nhập là khách hàng (không phải admin, không phải khách vãng lai). */
    public static boolean isCustomer(HttpServletRequest req) {
        var session = req.getSession(false);
        return session != null && session.getAttribute(CurrentUser.SESSION_KEY) != null
                && UserRole.CUSTOMER.databaseValue().equals(session.getAttribute(CurrentUser.ROLE_KEY));
    }

    /** Chỉ nhận đường dẫn nội bộ bắt đầu bằng "/" để không bị lợi dụng chuyển hướng sang trang lạ. */
    public static String safeBack(HttpServletRequest req, String fallback) {
        String back = req.getParameter("back");
        if (back == null || !back.startsWith("/") || back.startsWith("//") || back.contains("\\")
                || back.contains("\r") || back.contains("\n")) {
            return fallback;
        }
        return back;
    }

    /** Mã hoá giá trị để đặt lên query string; null thành chuỗi rỗng. */
    public static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
