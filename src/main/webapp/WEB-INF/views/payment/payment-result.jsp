<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<jsp:include page="/WEB-INF/views/common/header.jsp" />

<c:set var="payStatus" value="${not empty param.status ? param.status : 'paid'}" />
<c:set var="gateway" value="${not empty param.gateway ? param.gateway : 'vnpay'}" />
<c:set var="orderCode" value="${not empty param.orderCode ? param.orderCode : 'ORD-2026-0889'}" />

<div class="app-layout">
    <jsp:include page="/WEB-INF/views/common/sidebar.jsp" />

    <main class="main-content">
        <!-- THANH DEMO CHUYỂN TRẠNG THÁI KẾT QUẢ -->
        <section class="demo-toolbar">
            <div>
                <div class="demo-toolbar-title">🎛️ DEMO KẾT QUẢ CALLBACK CỔNG THANH TOÁN (F5: PaymentStatus)</div>
                <div style="font-size: 12px; color: #d4d4d8;">
                    Kiểm tra giao diện phản hồi khi Servlet nhận IPN/Return URL từ VNPay hoặc MoMo:
                </div>
            </div>
            <div style="display: flex; gap: 8px;">
                <a href="${pageContext.request.contextPath}/ui?page=payment-result&amp;status=paid&amp;gateway=${gateway}&amp;orderCode=${orderCode}"
                   class="btn btn-sm ${payStatus == 'paid' ? 'btn-primary' : 'btn-outline'}">
                    ✅ Trạng thái: PAID (Thành công)
                </a>
                <a href="${pageContext.request.contextPath}/ui?page=payment-result&amp;status=failed&amp;gateway=${gateway}&amp;orderCode=${orderCode}"
                   class="btn btn-sm ${payStatus == 'failed' ? 'btn-danger' : 'btn-outline'}">
                    ❌ Trạng thái: FAILED (Lỗi/Huỷ GD)
                </a>
            </div>
        </section>

        <c:choose>
            <c:when test="${payStatus == 'paid'}">
                <!-- TRẠNG THÁI THANH TOÁN THÀNH CÔNG -->
                <section class="brutal-card" style="background: linear-gradient(135deg, #dcfce7 0%, #ffffff 100%);">
                    <div style="display: flex; align-items: center; gap: 16px; margin-bottom: 18px;">
                        <div style="width: 64px; height: 64px; border-radius: 16px; background: #16a34a; color: #fff; border: 2px solid #111; box-shadow: 4px 4px 0 #111; display: flex; align-items: center; justify-content: center; font-size: 32px;">
                            ✔
                        </div>
                        <div>
                            <span class="badge badge-sale">Payment.markPaid() EXECUTED</span>
                            <h1 class="section-title" style="margin-top: 4px;">
                                THANH TOÁN THÀNH CÔNG — TIỀN ĐÃ ĐƯỢC TẠM GIỮ ESCROW!
                            </h1>
                            <p class="section-subtitle">
                                Đơn hàng <strong><c:out value="${orderCode}"/></strong> đã chuyển từ <code>awaiting_payment</code> sang <code>paid</code>. Người bán đã nhận thông báo chuẩn bị hàng.
                            </p>
                        </div>
                    </div>

                    <div class="kpi-grid" style="margin-bottom: 20px;">
                        <div class="kpi-card">
                            <div class="kpi-label">Mã giao dịch (Payment.code)</div>
                            <div class="kpi-value" style="font-size: 18px;">PAY-2026-99201</div>
                            <div style="font-size: 12px; color: #16a34a;">Đã lưu vào bảng <code>payments</code></div>
                        </div>
                        <div class="kpi-card">
                            <div class="kpi-label">Mã tham chiếu Cổng (gatewayTxnId)</div>
                            <div class="kpi-value" style="font-size: 18px;">VNP14289903</div>
                            <div style="font-size: 12px; color: #4b5563;">Cổng: <code><c:out value="${gateway}"/></code></div>
                        </div>
                        <div class="kpi-card">
                            <div class="kpi-label">Số tiền tạm giữ (amount)</div>
                            <div class="kpi-value" style="font-size: 18px; color: #16a34a;">1.330.000 đ</div>
                            <div style="font-size: 12px; color: #4b5563;">Bảo vệ 100% bởi Escrow</div>
                        </div>
                        <div class="kpi-card">
                            <div class="kpi-label">Thời gian thanh toán (paidAt)</div>
                            <div class="kpi-value" style="font-size: 16px;">11/10/2026 14:25</div>
                            <div style="font-size: 12px; color: #16a34a;">+35 EcoPoints tích luỹ</div>
                        </div>
                    </div>

                    <div style="display: flex; gap: 12px; flex-wrap: wrap;">
                        <a href="${pageContext.request.contextPath}/ui?page=shipment" class="btn btn-primary">
                            🚚 Theo dõi Vận đơn (F6 Shipment)
                        </a>
                        <a href="${pageContext.request.contextPath}/ui?page=dispute" class="btn btn-amber">
                            ⚖️ Giả lập Mở Khiếu nại / Hoàn tiền (F9 Dispute)
                        </a>
                        <a href="${pageContext.request.contextPath}/ui?page=payout" class="btn btn-outline">
                            💰 Xem Lệnh Giải ngân của Người bán (F9 Payout)
                        </a>
                    </div>
                </section>
            </c:when>

            <c:otherwise>
                <!-- TRẠNG THÁI THANH TOÁN THẤT BẠI -->
                <section class="brutal-card" style="background: linear-gradient(135deg, #fee2e2 0%, #ffffff 100%);">
                    <div style="display: flex; align-items: center; gap: 16px; margin-bottom: 18px;">
                        <div style="width: 64px; height: 64px; border-radius: 16px; background: #dc2626; color: #fff; border: 2px solid #111; box-shadow: 4px 4px 0 #111; display: flex; align-items: center; justify-content: center; font-size: 32px;">
                            ✖
                        </div>
                        <div>
                            <span class="badge badge-danger">Payment.markFailed() EXECUTED</span>
                            <h1 class="section-title" style="margin-top: 4px; color: #991b1b;">
                                GIAO DỊCH KHÔNG THÀNH CÔNG HOẶC BỊ HUỶ!
                            </h1>
                            <p class="section-subtitle">
                                Bản ghi <code>payments</code> đã cập nhật <code>status = 'failed'</code>. Đơn hàng <strong><c:out value="${orderCode}"/></strong> vẫn giữ ở trạng thái <code>awaiting_payment</code> để bạn có thể thanh toán lại.
                            </p>
                        </div>
                    </div>

                    <div style="background: #fff; border: 1.5px solid #111; border-radius: 12px; padding: 16px; margin-bottom: 18px; font-size: 13px;">
                        <div><strong>Mã lỗi cổng thanh toán:</strong> <code>ERR_GATEWAY_24 (Khách hàng huỷ giao dịch hoặc vượt hạn mức ngày)</code></div>
                        <div><strong>Cổng đã chọn:</strong> <code><c:out value="${gateway}"/></code> • <strong>Số tiền:</strong> 1.330.000 đ (Chưa bị trừ tiền)</div>
                    </div>

                    <div style="display: flex; gap: 12px; flex-wrap: wrap;">
                        <a href="${pageContext.request.contextPath}/ui?page=payment&amp;orderCode=${orderCode}" class="btn btn-primary">
                            🔄 Thử lại với cổng thanh toán khác
                        </a>
                        <a href="${pageContext.request.contextPath}/ui?page=orders" class="btn btn-outline">
                            🛒 Quay lại Danh sách Đơn hàng (F4)
                        </a>
                    </div>
                </section>
            </c:otherwise>
        </c:choose>
    </main>
</div>

<jsp:include page="/WEB-INF/views/common/footer.jsp" />
