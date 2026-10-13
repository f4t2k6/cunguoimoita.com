package org.example.cunguoimoita.common;

import java.io.IOException;
import java.io.PrintWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Bản demo giao diện (/ui), gộp từ nhánh main. Trang chủ chính là /home (HomeServlet).
 * Controller điều hướng giao diện 10 chức năng dự án (F1 -> F10)
 * và xuất báo cáo đối soát giải ngân CSV (F9 Payout - Tuấn Anh).
 */
@WebServlet(name = "DemoUiServlet", urlPatterns = {"/ui", "/payout-export"})
public class DemoUiServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String servletPath = request.getServletPath();
        if ("/payout-export".equals(servletPath)) {
            exportPayoutCsv(response);
            return;
        }

        String page = request.getParameter("page");
        if (page == null || page.trim().isEmpty()) {
            page = "home";
        }

        String viewPath;
        String pageTitle;

        switch (page) {
            case "profile":
                viewPath = "/WEB-INF/views/account/profile.jsp";
                pageTitle = "F1: Hồ Sơ & Sổ Địa Chỉ — Cũ Người Mới Ta";
                break;
            case "product-detail":
                viewPath = "/WEB-INF/views/product/detail.jsp";
                pageTitle = "F2: Chi Tiết & Đăng Sản Phẩm — Cũ Người Mới Ta";
                break;
            case "wishlist":
                viewPath = "/WEB-INF/views/feed/wishlist.jsp";
                pageTitle = "F3: Bucket List & Gợi Ý Trao Đổi — Cũ Người Mới Ta";
                break;
            case "orders":
                viewPath = "/WEB-INF/views/order/orders.jsp";
                pageTitle = "F4: Giỏ Hàng & Đơn Hàng — Cũ Người Mới Ta";
                break;
            case "payment":
                viewPath = "/WEB-INF/views/payment/checkout-gateway.jsp";
                pageTitle = "F5: Cổng Thanh Toán Escrow (Tuấn Anh) — Cũ Người Mới Ta";
                break;
            case "payment-result":
                viewPath = "/WEB-INF/views/payment/payment-result.jsp";
                pageTitle = "F5: Kết Quả Giao Dịch (Tuấn Anh) — Cũ Người Mới Ta";
                break;
            case "shipment":
                viewPath = "/WEB-INF/views/shipping/shipment.jsp";
                pageTitle = "F6: Vận Đơn & Đối Soát COD — Cũ Người Mới Ta";
                break;
            case "messages":
                viewPath = "/WEB-INF/views/chat/messages.jsp";
                pageTitle = "F7: Tin Nhắn & Thương Lượng Giá — Cũ Người Mới Ta";
                break;
            case "rewards":
                viewPath = "/WEB-INF/views/review/tier-rewards.jsp";
                pageTitle = "F8: Đánh Giá & Hạng Thành Viên EcoPoints — Cũ Người Mới Ta";
                break;
            case "dispute":
                viewPath = "/WEB-INF/views/dispute/dispute-center.jsp";
                pageTitle = "F9: Trung Tâm Khiếu Nại & Hoàn Tiền (Tuấn Anh) — Cũ Người Mới Ta";
                break;
            case "payout":
                viewPath = "/WEB-INF/views/dispute/seller-payout.jsp";
                pageTitle = "F9: Ví Bán Hàng & Giải Ngân Payout (Tuấn Anh) — Cũ Người Mới Ta";
                break;
            case "admin":
                response.sendRedirect(request.getContextPath() + "/admin/dashboard");
                return;
            case "home":
            default:
                page = "home";
                viewPath = "/WEB-INF/views/feed/demo-home.jsp";
                pageTitle = "Cũ Người Mới Ta — Sàn Mua Bán & Trao Đổi Đồ Cũ Eco";
                break;
        }

        request.setAttribute("currentPage", page);
        request.setAttribute("pageTitle", pageTitle);
        request.setAttribute("currentUserEcoPoints", "1,450");
        request.setAttribute("currentUserTier", "GOLD TIER (-3% PHÍ)");

        request.getRequestDispatcher(viewPath).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }

    private void exportPayoutCsv(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"payout_report_tuananh_f9.csv\"");

        try (PrintWriter out = response.getWriter()) {
            out.write('\ufeff');
            out.println("Payout ID,Order Code,Seller ID,Gross Amount (VND),Commission Fee (VND),Net Amount (VND),Method,Status,Payout Date");
            out.println("PAYOUT-501,ORD-2026-0895,4,2800000,84000,2716000,bank_transfer,scheduled,2026-10-14 09:00");
            out.println("PAYOUT-498,ORD-2026-0840,4,3400000,102000,3298000,bank_transfer,done,2026-10-10 09:15");
            out.println("PAYOUT-472,ORD-2026-0790,4,1950000,58500,1891500,ewallet,done,2026-10-06 16:40");
            out.println("PAYOUT-455,ORD-2026-0712,4,950000,28500,921500,bank_transfer,failed,2026-10-02 11:20");
        }
    }
}
