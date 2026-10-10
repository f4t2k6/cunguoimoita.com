<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<jsp:include page="/WEB-INF/views/common/header.jsp" />

<div class="app-layout">
    <jsp:include page="/WEB-INF/views/common/sidebar.jsp" />

    <main class="main-content">
        <section class="brutal-card">
            <div class="section-header">
                <div>
                    <span class="badge badge-verified" style="margin-bottom: 6px;">PHÂN HỆ F10 • PACKAGE: org.example.cunguoimoita.admin</span>
                    <h1 class="section-title">🛡️ BẢNG ĐIỀU KHIỂN QUẢN TRỊ VIÊN &amp; TOÀ ÁN TRỌNG TÀI (ADMIN &amp; REPORT)</h1>
                    <p class="section-subtitle">
                        Thực thi các quyền của <code>Admin extends User</code>: <code>resolveDispute()</code> (liên kết trực tiếp F9 của Tuấn Anh), <code>approveProduct()</code>, <code>banUser()</code> và xử lý <code>Report</code>
                    </p>
                </div>
                <span class="badge badge-danger">permissionLevel = SUPER_ADMIN</span>
            </div>

            <!-- HÀNG ĐỢI TRỌNG TÀI XỬ LÝ TRANH CHẤP F9 (Admin.resolveDispute()) -->
            <h2 style="font-family: 'Bungee', sans-serif; font-size: 16px; margin: 10px 0 12px;">
                ⚖️ 1. HÀNG ĐỢI PHÁN QUYẾT KHIẾU NẠI (KẾT NỐI TRỰC TIẾP VỚI F9 DISPUTE/REFUND/PAYOUT)
            </h2>
            <div class="table-wrapper" style="margin-bottom: 24px;">
                <table class="brutal-table">
                    <thead>
                        <tr>
                            <th>Mã Khiếu Nại</th>
                            <th>Đơn Hàng</th>
                            <th>Người Mua / Người Bán</th>
                            <th>Lý Do (DisputeReason)</th>
                            <th>Số Tiền Tạm Giữ</th>
                            <th>Phán Quyết Của Admin (F10 → F9)</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td><strong>#DSP-2026-041</strong></td>
                            <td><code>ORD-2026-0889</code></td>
                            <td>Tuấn Anh vs Minh Khoa</td>
                            <td><span class="badge badge-danger">damaged</span></td>
                            <td><strong>1.330.000 đ</strong></td>
                            <td style="display: flex; gap: 8px; flex-wrap: wrap;">
                                <a href="${pageContext.request.contextPath}/ui?page=dispute&amp;state=resolved_refund" class="btn btn-primary btn-sm">
                                    ✔ Buyer Thắng → Tạo Refund (F9)
                                </a>
                                <a href="${pageContext.request.contextPath}/ui?page=dispute&amp;state=resolved_payout" class="btn btn-amber btn-sm">
                                    🏪 Seller Thắng → Giải ngân Payout (F9)
                                </a>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <!-- DUYỆT SẢN PHẨM & BÁO CÁO VI PHẠM (approveProduct & Report) -->
            <div class="split-equal">
                <div style="background: #f4f4f0; border: 1.5px solid #111; border-radius: 14px; padding: 16px;">
                    <h3 style="font-family: 'Bungee', sans-serif; font-size: 15px; margin-top: 0;">
                        📦 2. DUYỆT KIỂM ĐỊNH SẢN PHẨM (<code>approveProduct</code>)
                    </h3>
                    <div class="brutal-card" style="padding: 12px; display: flex; justify-content: space-between; align-items: center;">
                        <div>
                            <strong>💻 MacBook Air M1 2020 8GB/256GB</strong>
                            <div style="font-size: 12px; color: #4b5563;">Người đăng: Hoàng Nam • Giá: 11.500.000 đ</div>
                        </div>
                        <span class="badge badge-verified">✔ Đã cấp tích xanh</span>
                    </div>
                </div>

                <div style="background: #fee2e2; border: 1.5px solid #111; border-radius: 14px; padding: 16px;">
                    <h3 style="font-family: 'Bungee', sans-serif; font-size: 15px; margin-top: 0; color: #991b1b;">
                        🚨 3. BÁO CÁO VI PHẠM (<code>Report.resolve</code> &amp; <code>banUser</code>)
                    </h3>
                    <div class="brutal-card" style="padding: 12px; display: flex; justify-content: space-between; align-items: center;">
                        <div>
                            <strong>Tài khoản #98 (bom_hang_user)</strong>
                            <div style="font-size: 12px; color: #7f1d1d;">Từ chối nhận COD 3 lần liên tiếp (<code>codRefusalCount = 3</code>)</div>
                        </div>
                        <span class="badge badge-danger">Đã khoá (banned)</span>
                    </div>
                </div>
            </div>
        </section>
    </main>
</div>

<jsp:include page="/WEB-INF/views/common/footer.jsp" />
