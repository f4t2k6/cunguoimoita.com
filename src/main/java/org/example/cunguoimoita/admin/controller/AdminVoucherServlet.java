package org.example.cunguoimoita.admin.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.cunguoimoita.order.service.VoucherService;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Quản lý voucher (F4 + F10). Quyền admin do AdminAuthFilter kiểm tra.
 * GET  /admin/vouchers → form phát voucher + danh sách đã phát. Attribute gửi sang JSP: vouchers (List&lt;VoucherRow&gt;)
 * POST /admin/vouchers  field: code, email (trống = mọi khách), discountValue, minOrderValue, expiryDate (yyyy-MM-dd, trống = không hạn)
 */
@WebServlet("/admin/vouchers")
public class AdminVoucherServlet extends AdminBaseServlet {

    private final VoucherService voucherService = new VoucherService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            req.setAttribute("vouchers", voucherService.listRecent());
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        render(req, resp, "vouchers.jsp", "vouchers");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        handle(req, resp, "/admin/vouchers", () -> {
            int count = voucherService.issue(req.getParameter("code"), req.getParameter("email"),
                    number(req.getParameter("discountValue")), number(req.getParameter("minOrderValue")),
                    date(req.getParameter("expiryDate")));
            return "Đã phát " + count + " voucher";
        });
    }

    private static double number(String value) {
        try {
            return value == null || value.isBlank() ? 0 : Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Số tiền không hợp lệ");
        }
    }

    private static LocalDate date(String value) {
        try {
            return value == null || value.isBlank() ? null : LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Ngày hết hạn không hợp lệ");
        }
    }
}
