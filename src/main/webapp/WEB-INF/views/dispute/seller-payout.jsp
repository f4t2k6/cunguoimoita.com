<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>

<jsp:include page="/WEB-INF/views/common/demo-header.jsp" />

<c:set var="tierRate" value="${not empty param.tierRate ? param.tierRate : '3'}" />

<div class="app-layout">
    <jsp:include page="/WEB-INF/views/common/sidebar.jsp" />

    <main class="main-content">
        <!-- THANH DEMO TÍNH PHÍ HOA HỒNG THEO HẠNG THÀNH VIÊN (F9 PAYOUT + F8 MEMBER TIER) -->
        <section class="demo-toolbar">
            <div>
                <div class="demo-toolbar-title">🎛️ BẢNG ĐIỀU KHIỂN DEMO F9: GIẢI NGÂN NGƯỜI BÁN (PAYOUT &amp; COMMISSION CALCULATOR)</div>
                <div style="font-size: 12px; color: #d4d4d8;">
                    Thử nghiệm hàm <code>Payout.calculateNetAmount()</code> trừ phí sàn theo <code>MemberTier.discountRate</code>:
                </div>
            </div>
            <div style="display: flex; gap: 8px; flex-wrap: wrap;">
                <a href="${pageContext.request.contextPath}/ui?page=payout&amp;tierRate=5"
                   class="btn btn-sm ${tierRate == '5' ? 'btn-amber' : 'btn-outline'}">
                    🥉 Hạng Bronze (Phí sàn 5%)
                </a>
                <a href="${pageContext.request.contextPath}/ui?page=payout&amp;tierRate=4"
                   class="btn btn-sm ${tierRate == '4' ? 'btn-amber' : 'btn-outline'}">
                    🥈 Hạng Silver (Phí sàn 4%)
                </a>
                <a href="${pageContext.request.contextPath}/ui?page=payout&amp;tierRate=3"
                   class="btn btn-sm ${tierRate == '3' ? 'btn-primary' : 'btn-outline'}">
                    🥇 Hạng Gold (Phí sàn 3% - Hiện tại)
                </a>
                <a href="${pageContext.request.contextPath}/ui?page=payout&amp;tierRate=1.5"
                   class="btn btn-sm ${tierRate == '1.5' ? 'btn-dark' : 'btn-outline'}">
                    💎 Hạng Diamond (Phí sàn 1.5%)
                </a>
            </div>
        </section>

        <!-- HEADER & NÚT XUẤT EXCEL/CSV BÁO CÁO ĐỐI SOÁT (CHAPTER 18 HTTP HEADERS) -->
        <section class="brutal-card">
            <div class="section-header" style="margin-bottom: 16px;">
                <div>
                    <span class="badge badge-verified" style="margin-bottom: 6px;">PHÂN HỆ F9 • PACKAGE: org.example.cunguoimoita.dispute</span>
                    <h1 class="section-title">💰 VÍ DOANH THU NGƯỜI BÁN &amp; ĐỐI SOÁT GIẢI NGÂN (SELLER PAYOUT)</h1>
                    <p class="section-subtitle">
                        Quản lý bảng <code>payouts</code>: Tự động khấu trừ <code>commissionFee</code> từ <code>grossAmount</code> để tính <code>netAmount</code> thực nhận
                    </p>
                </div>
                <div style="display: flex; gap: 10px; flex-wrap: wrap;">
                    <a href="${pageContext.request.contextPath}/payout-export" class="btn btn-primary">
                        📥 Xuất Báo Cáo Đối Soát CSV (Servlet Header)
                    </a>
                    <a href="${pageContext.request.contextPath}/ui?page=profile" class="btn btn-outline">
                        🏦 Cấu hình TK nhận tiền (payoutAccountRef)
                    </a>
                </div>
            </div>

            <!-- 4 THẺ KPI TÀI CHÍNH CỦA NGƯỜI BÁN -->
            <div class="kpi-grid">
                <div class="kpi-card" style="background: #dcfce7;">
                    <div class="kpi-label">Đã giải ngân thành công (done)</div>
                    <div class="kpi-value" style="color: #14532d;">8.420.000 đ</div>
                    <div style="font-size: 12px; color: #065f46;">14 đơn hàng đã tất toán về Vietcombank</div>
                </div>
                <div class="kpi-card" style="background: #fef9c3;">
                    <div class="kpi-label">Đang chờ giải ngân (scheduled)</div>
                    <div class="kpi-value" style="color: #854d0e;">3.150.000 đ</div>
                    <div style="font-size: 12px; color: #713f12;">3 đơn đang trong thời gian chờ 3 ngày</div>
                </div>
                <div class="kpi-card">
                    <div class="kpi-label">Tổng phí sàn đã đóng (commissionFee)</div>
                    <div class="kpi-value">357.000 đ</div>
                    <div style="font-size: 12px; color: #16a34a;">Đã tiết kiệm 240.000 đ nhờ Gold Tier</div>
                </div>
                <div class="kpi-card">
                    <div class="kpi-label">Tài khoản thụ hưởng (payoutAccountRef)</div>
                    <div class="kpi-value" style="font-size: 16px;">VCB • 1029384756</div>
                    <div style="font-size: 12px; color: #4b5563;">Chủ TK: NGUYEN TUAN ANH (Đã xác thực)</div>
                </div>
            </div>
        </section>

        <!-- CÔNG CỤ MÔ PHỎNG HÀM Payout.calculateNetAmount() -->
        <section class="brutal-card" style="background: #fffef8;">
            <h2 class="section-title" style="font-size: 17px; margin-bottom: 12px;">
                🧮 MÔ PHỎNG CÔNG THỨC <code>Payout.calculateNetAmount()</code> TRÊN ĐƠN HÀNG MẪU (2.000.000 đ)
            </h2>
            <div class="kpi-grid">
                <div class="kpi-card">
                    <div class="kpi-label">1. Doanh thu gốc (grossAmount)</div>
                    <div class="kpi-value">2.000.000 đ</div>
                    <div style="font-size: 12px; color: #4b5563;">Giá bán sản phẩm trước phí</div>
                </div>
                <div class="kpi-card">
                    <div class="kpi-label">2. Phí hoa hồng (commissionFee - <c:out value="${tierRate}"/>%)</div>
                    <div class="kpi-value" style="color: #dc2626;">
                        <c:choose>
                            <c:when test="${tierRate == '5'}">-100.000 đ</c:when>
                            <c:when test="${tierRate == '4'}">-80.000 đ</c:when>
                            <c:when test="${tierRate == '1.5'}">-30.000 đ</c:when>
                            <c:otherwise>-60.000 đ</c:otherwise>
                        </c:choose>
                    </div>
                    <div style="font-size: 12px; color: #4b5563;">Áp dụng ưu đãi <code>MemberTier</code> (F8)</div>
                </div>
                <div class="kpi-card" style="background: #dcfce7; border-color: #16a34a;">
                    <div class="kpi-label">3. Thực nhận về ví (netAmount)</div>
                    <div class="kpi-value" style="color: #16a34a;">
                        <c:choose>
                            <c:when test="${tierRate == '5'}">1.900.000 đ</c:when>
                            <c:when test="${tierRate == '4'}">1.920.000 đ</c:when>
                            <c:when test="${tierRate == '1.5'}">1.970.000 đ</c:when>
                            <c:otherwise>1.940.000 đ</c:otherwise>
                        </c:choose>
                    </div>
                    <div style="font-size: 12px; color: #14532d;"><code>netAmount = grossAmount - commissionFee</code></div>
                </div>
            </div>
        </section>

        <!-- BẢNG LỊCH SỬ GIẢI NGÂN CHI TIẾT (BẢNG PAYOUTS) -->
        <section class="brutal-card">
            <div class="section-header">
                <div>
                    <h2 class="section-title" style="font-size: 18px;">📋 BẢNG ĐỐI SOÁT LỆNH GIẢI NGÂN (BẢNG PAYOUTS)</h2>
                    <p class="section-subtitle">
                        Hiển thị đầy đủ các cột: <code>id</code>, <code>order_id</code>, <code>gross_amount</code>, <code>commission_fee</code>, <code>net_amount</code>, <code>method</code>, <code>status</code>, <code>payout_date</code>
                    </p>
                </div>
            </div>

            <div class="table-wrapper">
                <table class="brutal-table">
                    <thead>
                        <tr>
                            <th>Mã Payout</th>
                            <th>Mã Đơn Hàng</th>
                            <th>Doanh Thu Gốc (Gross)</th>
                            <th>Phí Hoa Hồng (Fee)</th>
                            <th>Thực Nhận (Net)</th>
                            <th>Phương Thức (Method)</th>
                            <th>Trạng Thái (Status)</th>
                            <th>Ngày Giải Ngân</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td><strong>#PAYOUT-501</strong></td>
                            <td><code>ORD-2026-0895</code></td>
                            <td>2.800.000 đ</td>
                            <td style="color: #dc2626;">-84.000 đ (3%)</td>
                            <td><strong style="color: #16a34a;">2.716.000 đ</strong></td>
                            <td><code>bank_transfer</code></td>
                            <td><span class="badge badge-swap">scheduled</span></td>
                            <td>Dự kiến 14/10/2026</td>
                        </tr>
                        <tr>
                            <td><strong>#PAYOUT-498</strong></td>
                            <td><code>ORD-2026-0840</code></td>
                            <td>3.400.000 đ</td>
                            <td style="color: #dc2626;">-102.000 đ (3%)</td>
                            <td><strong style="color: #16a34a;">3.298.000 đ</strong></td>
                            <td><code>bank_transfer</code></td>
                            <td><span class="badge badge-sale">done</span></td>
                            <td>10/10/2026 09:15</td>
                        </tr>
                        <tr>
                            <td><strong>#PAYOUT-472</strong></td>
                            <td><code>ORD-2026-0790</code></td>
                            <td>1.950.000 đ</td>
                            <td style="color: #dc2626;">-58.500 đ (3%)</td>
                            <td><strong style="color: #16a34a;">1.891.500 đ</strong></td>
                            <td><code>ewallet (MoMo)</code></td>
                            <td><span class="badge badge-sale">done</span></td>
                            <td>06/10/2026 16:40</td>
                        </tr>
                        <tr>
                            <td><strong>#PAYOUT-455</strong></td>
                            <td><code>ORD-2026-0712</code></td>
                            <td>950.000 đ</td>
                            <td style="color: #dc2626;">-28.500 đ (3%)</td>
                            <td><strong style="color: #16a34a;">921.500 đ</strong></td>
                            <td><code>bank_transfer</code></td>
                            <td><span class="badge badge-danger">failed (Sai số TK)</span></td>
                            <td>02/10/2026 (Đã thử lại)</td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </section>
    </main>
</div>

<jsp:include page="/WEB-INF/views/common/demo-footer.jsp" />
