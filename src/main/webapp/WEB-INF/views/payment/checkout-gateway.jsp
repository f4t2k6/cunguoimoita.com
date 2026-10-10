<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<jsp:include page="/WEB-INF/views/common/header.jsp" />

<c:set var="scenario" value="${not empty param.scenario ? param.scenario : 'normal'}" />
<c:set var="selectedGateway" value="${not empty param.gateway ? param.gateway : 'vnpay'}" />
<c:set var="orderCode" value="${not empty param.orderCode ? param.orderCode : 'ORD-2026-0889'}" />

<div class="app-layout">
    <jsp:include page="/WEB-INF/views/common/sidebar.jsp" />

    <main class="main-content">
        <!-- THANH CÔNG CỤ DEMO KỊCH BẢN TRÊN LỚP (DÀNH CHO TUẤN ANH - F5) -->
        <section class="demo-toolbar">
            <div>
                <div class="demo-toolbar-title">🎛️ BẢNG ĐIỀU KHIỂN KỊCH BẢN DEMO F5 (TUẤN ANH - PAYMENT &amp; COD GUARD)</div>
                <div style="font-size: 12px; color: #d4d4d8;">
                    Chuyển nhanh trạng thái để thuyết trình hàm <code>Customer.canUseCod()</code>, chọn cổng <code>PaymentGateway</code> và xử lý Callback:
                </div>
            </div>
            <div style="display: flex; gap: 8px; flex-wrap: wrap;">
                <a href="${pageContext.request.contextPath}/ui?page=payment&amp;scenario=normal&amp;gateway=vnpay"
                   class="btn btn-sm ${scenario == 'normal' ? 'btn-primary' : 'btn-outline'}">
                    ✅ Kịch bản 1: Khách đủ điều kiện (Cho phép COD &amp; Online)
                </a>
                <a href="${pageContext.request.contextPath}/ui?page=payment&amp;scenario=cod_blocked&amp;gateway=momo"
                   class="btn btn-sm ${scenario == 'cod_blocked' ? 'btn-danger' : 'btn-outline'}">
                    🚫 Kịch bản 2: Khách bom hàng ≥ 2 lần (Khoá COD tự động)
                </a>
                <a href="${pageContext.request.contextPath}/ui?page=payment-result&amp;status=paid&amp;gateway=${selectedGateway}&amp;orderCode=${orderCode}"
                   class="btn btn-sm btn-amber">
                    🎉 Test Callback Thành Công (PAID)
                </a>
                <a href="${pageContext.request.contextPath}/ui?page=payment-result&amp;status=failed&amp;gateway=${selectedGateway}&amp;orderCode=${orderCode}"
                   class="btn btn-sm btn-outline">
                    ❌ Test Callback Thất Bại (FAILED)
                </a>
            </div>
        </section>

        <!-- HEADER PHÂN HỆ F5 -->
        <section class="brutal-card">
            <div class="section-header" style="margin-bottom: 0;">
                <div>
                    <span class="badge badge-verified" style="margin-bottom: 6px;">PHÂN HỆ F5 • PACKAGE: org.example.cunguoimoita.payment</span>
                    <h1 class="section-title">💳 CỔNG THANH TOÁN TẠM GIỮ TIỀN AN TOÀN (ESCROW GATEWAY)</h1>
                    <p class="section-subtitle">
                        Thực thi phương thức <code>Payment.createPayment()</code>, <code>Payment.markPaid()</code>, <code>Payment.markFailed()</code> kết hợp kiểm tra <code>Customer.canUseCod()</code>
                    </p>
                </div>
                <div style="text-align: right;">
                    <div style="font-size: 12px; color: #4b5563;">Mã đơn hàng đang thanh toán</div>
                    <div style="font-family: 'Bungee', sans-serif; font-size: 20px; color: #16a34a;">
                        <c:out value="${orderCode}" />
                    </div>
                </div>
            </div>
        </section>

        <!-- BỐ CỤC 2 CỘT: CHỌN CỔNG THANH TOÁN (TRÁI) + BIÊN LAI ESCROW TICKET (PHẢI) -->
        <div class="split-2col">
            <!-- CỘT TRÁI: CHỌN PHƯƠNG THỨC & FORM XÁC NHẬN -->
            <div class="brutal-card">
                <h2 class="section-title" style="font-size: 17px; margin-bottom: 14px;">
                    1. KIỂM TRA ĐIỀU KIỆN NGƯỜI MUA &amp; CHỌN PHƯƠNG THỨC
                </h2>

                <!-- HỘP KIỂM TRA ĐIỀU KIỆN COD (Customer.canUseCod()) -->
                <c:choose>
                    <c:when test="${scenario == 'cod_blocked'}">
                        <div style="background: #fee2e2; border: 2px solid #dc2626; border-radius: 12px; padding: 14px; margin-bottom: 18px;">
                            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px;">
                                <strong style="color: #991b1b;">🚫 CẢNH BÁO HỆ THỐNG: TỰ ĐỘNG KHOÁ PHƯƠNG THỨC COD</strong>
                                <span class="badge badge-danger">canUseCod() = FALSE</span>
                            </div>
                            <div style="font-size: 13px; color: #7f1d1d;">
                                Tài khoản người mua hiện có <code>codRefusalCount = 2</code> (vượt ngưỡng cho phép &lt; 2) và <code>outstandingDebt = 35.000 đ</code> (phí hoàn hàng chưa thanh toán). Hệ thống bắt buộc thanh toán trả trước qua cổng Online (VNPay / MoMo / Chuyển khoản).
                            </div>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div style="background: #dcfce7; border: 2px solid #16a34a; border-radius: 12px; padding: 14px; margin-bottom: 18px;">
                            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px;">
                                <strong style="color: #14532d;">✔ KIỂM TRA TÍN NHIỆM NGƯỜI MUA HỢP LỆ</strong>
                                <span class="badge badge-sale">canUseCod() = TRUE</span>
                            </div>
                            <div style="font-size: 13px; color: #065f46;">
                                Người mua đã xác thực SĐT (<code>isPhoneVerified = true</code>), lịch sử bom hàng <code>codRefusalCount = 0</code>, công nợ <code>outstandingDebt = 0 đ</code>. Được phép dùng cả Online Escrow lẫn COD.
                            </div>
                        </div>
                    </c:otherwise>
                </c:choose>

                <!-- FORM GỬI TỚI SERVLET XỬ LÝ THANH TOÁN -->
                <form action="${pageContext.request.contextPath}/ui" method="get">
                    <input type="hidden" name="page" value="payment-result" />
                    <input type="hidden" name="orderCode" value="<c:out value='${orderCode}'/>" />
                    <input type="hidden" name="status" value="paid" />

                    <!-- Option 1: VNPay -->
                    <label class="gateway-option ${selectedGateway == 'vnpay' ? 'selected' : ''}">
                        <div style="display: flex; align-items: center; gap: 12px;">
                            <input type="radio" name="gateway" value="vnpay" ${selectedGateway == 'vnpay' ? 'checked' : ''} />
                            <div>
                                <div style="font-weight: 700; font-size: 15px;">🏦 Cổng VNPay QR / Thẻ ATM Nội Địa &amp; Quốc Tế</div>
                                <div style="font-size: 12px; color: #4b5563;">
                                    Enum: <code>gateway = 'vnpay'</code> • Tự động đối soát <code>gatewayTxnId</code> tức thì
                                </div>
                            </div>
                        </div>
                        <span class="badge badge-sale">Khuyên dùng</span>
                    </label>

                    <!-- Option 2: MoMo -->
                    <label class="gateway-option ${selectedGateway == 'momo' ? 'selected' : ''}">
                        <div style="display: flex; align-items: center; gap: 12px;">
                            <input type="radio" name="gateway" value="momo" ${selectedGateway == 'momo' ? 'checked' : ''} />
                            <div>
                                <div style="font-weight: 700; font-size: 15px;">👛 Ví Điện Tử MoMo (Siêu Ứng Dụng Thanh Toán)</div>
                                <div style="font-size: 12px; color: #4b5563;">
                                    Enum: <code>gateway = 'momo'</code> • Quét mã QR MoMo nhận thêm +15 EcoPoints
                                </div>
                            </div>
                        </div>
                        <span class="badge badge-swap">+15 EcoPoints</span>
                    </label>

                    <!-- Option 3: Bank Transfer VietQR -->
                    <label class="gateway-option ${selectedGateway == 'bank_transfer' ? 'selected' : ''}">
                        <div style="display: flex; align-items: center; gap: 12px;">
                            <input type="radio" name="gateway" value="bank_transfer" ${selectedGateway == 'bank_transfer' ? 'checked' : ''} />
                            <div>
                                <div style="font-weight: 700; font-size: 15px;">📲 Chuyển Khoản Ngân Hàng VietQR 24/7 (Escrow)</div>
                                <div style="font-size: 12px; color: #4b5563;">
                                    Enum: <code>gateway = 'bank_transfer'</code> • Cú pháp tự động: <code>CNMT <c:out value="${orderCode}"/></code>
                                </div>
                            </div>
                        </div>
                        <span class="badge badge-both">Miễn phí GD</span>
                    </label>

                    <!-- Option 4: COD -->
                    <c:choose>
                        <c:when test="${scenario == 'cod_blocked'}">
                            <div class="gateway-option disabled">
                                <div style="display: flex; align-items: center; gap: 12px;">
                                    <input type="radio" disabled />
                                    <div>
                                        <div style="font-weight: 700; font-size: 15px; color: #991b1b;">
                                            🚚 Thanh toán khi nhận hàng (COD) — ĐÃ BỊ KHOÁ
                                        </div>
                                        <div style="font-size: 12px; color: #7f1d1d;">
                                            Bị vô hiệu hoá do <code>Customer.canUseCod() == false</code> (Từ chối nhận hàng COD ≥ 2 lần)
                                        </div>
                                    </div>
                                </div>
                                <span class="badge badge-danger">Đã khoá</span>
                            </div>
                        </c:when>
                        <c:otherwise>
                            <label class="gateway-option">
                                <div style="display: flex; align-items: center; gap: 12px;">
                                    <input type="radio" name="gateway" value="cod" />
                                    <div>
                                        <div style="font-weight: 700; font-size: 15px;">🚚 Thanh toán tiền mặt khi nhận hàng (COD - F6)</div>
                                        <div style="font-size: 12px; color: #4b5563;">
                                            Áp dụng cho sản phẩm có <code>Product.acceptCod = true</code> • Thu hộ qua GHN/GHTK
                                        </div>
                                    </div>
                                </div>
                                <span class="badge badge-pending">Khả dụng</span>
                            </label>
                        </c:otherwise>
                    </c:choose>

                    <!-- MÔ PHỎNG MÃ QR & THÔNG TIN GIAO DỊCH -->
                    <div style="background: #f4f4f0; border: 1.5px solid #111; border-radius: 12px; padding: 16px; margin: 16px 0; display: grid; grid-template-columns: 120px 1fr; gap: 16px; align-items: center;">
                        <div style="width: 120px; height: 120px; background: #fff; border: 2px solid #111; border-radius: 10px; display: flex; flex-direction: column; align-items: center; justify-content: center; text-align: center; padding: 8px;">
                            <div style="font-family: 'Bungee', sans-serif; font-size: 11px;">VIETQR / VNPAY</div>
                            <div style="font-size: 32px; line-height: 1; margin: 4px 0;">▣▤▥</div>
                            <div style="font-size: 10px; color: #16a34a; font-weight: 700;">SCAN TO PAY</div>
                        </div>
                        <div style="font-size: 13px;">
                            <div style="font-weight: 700; margin-bottom: 4px;">📌 Thông tin bản ghi <code>payments</code> chuẩn bị khởi tạo:</div>
                            <div>• Mã giao dịch nội bộ (<code>code</code>): <strong>PAY-2026-99201</strong></div>
                            <div>• Trạng thái khởi tạo (<code>status</code>): <span class="badge badge-pending">pending</span></div>
                            <div>• Tài khoản thụ hưởng tạm giữ: <strong>CÔNG TY CŨ NGƯỜI MỚI TA (ESCROW HUB)</strong></div>
                            <div>• Nội dung chuyển khoản: <code>CNMT <c:out value="${orderCode}"/></code></div>
                        </div>
                    </div>

                    <div style="display: flex; gap: 12px; flex-wrap: wrap;">
                        <button type="submit" class="btn btn-primary" style="flex: 1;">
                            🔒 Xác nhận Thanh toán 1.330.000 đ (markPaid)
                        </button>
                        <a href="${pageContext.request.contextPath}/ui?page=payment-result&amp;status=failed&amp;gateway=${selectedGateway}&amp;orderCode=${orderCode}"
                           class="btn btn-danger">
                            Giả lập Lỗi Cổng TT (markFailed)
                        </a>
                    </div>
                </form>
            </div>

            <!-- CỘT PHẢI: BIÊN LAI TẠM GIỮ TIỀN ESCROW (RECEIPT TICKET) -->
            <aside class="receipt-ticket">
                <div class="receipt-header">
                    <div>
                        <div style="font-family: 'Bungee', sans-serif; font-size: 16px;">ESCROW RECEIPT TICKET</div>
                        <div style="font-size: 12px; color: #4b5563;">Bảo chứng bởi Cũ Người Mới Ta</div>
                    </div>
                    <span class="badge badge-verified">F5 ESCROW</span>
                </div>

                <div style="margin-bottom: 12px; padding-bottom: 12px; border-bottom: 1px dashed #a1a1aa;">
                    <div style="font-size: 12px; color: #4b5563;">Sản phẩm thanh toán:</div>
                    <div style="font-weight: 700; font-size: 14px; margin-top: 2px;">
                        ⌨️ Bàn phím cơ Keychron K2 Pro RGB Hotswap Red Switch
                    </div>
                    <div style="font-size: 12px; color: #4b5563; margin-top: 4px;">
                        Người bán: <strong>Minh Khoa (ID #14)</strong> • Người mua: <strong>Tuấn Anh (ID #04)</strong>
                    </div>
                </div>

                <div class="receipt-row">
                    <span>Giá trị sản phẩm (<code>subTotal</code>)</span>
                    <strong>1.350.000 đ</strong>
                </div>
                <div class="receipt-row">
                    <span>Phí vận chuyển GHN (<code>shippingFee</code>)</span>
                    <strong>30.000 đ</strong>
                </div>
                <div class="receipt-row" style="color: #16a34a;">
                    <span>Voucher EcoPoints (<code>ECO50K</code>)</span>
                    <strong>-50.000 đ</strong>
                </div>

                <div class="receipt-total">
                    <span>TỔNG THANH TOÁN</span>
                    <span>1.330.000 đ</span>
                </div>

                <div style="margin-top: 16px; background: #dcfce7; border: 1.5px solid #111; border-radius: 10px; padding: 12px; font-size: 12px;">
                    <div style="font-weight: 700; color: #14532d; margin-bottom: 4px;">
                        🛡️ Cơ chế Bảo vệ Người mua &amp; Người bán (F5 → F9):
                    </div>
                    <ol style="margin: 0; padding-left: 18px; color: #065f46;">
                        <li>Khoản tiền <strong>1.330.000 đ</strong> được hệ thống giữ ở trạng thái <code>paid</code>.</li>
                        <li>Người bán giao hàng qua đơn vị vận chuyển (F6).</li>
                        <li>Nếu hàng đúng mô tả: Hệ thống tự động tạo lệnh <strong>Payout (F9)</strong> giải ngân cho người bán.</li>
                        <li>Nếu hàng lỗi/giả: Người mua bấm <strong>Mở Khiếu nại (F9)</strong> để nhận <strong>Refund 100%</strong>.</li>
                    </ol>
                </div>

                <!-- BẢNG LỊCH SỬ GIAO DỊCH PAYMENT GẦN ĐÂY -->
                <div style="margin-top: 18px;">
                    <div style="font-family: 'Bungee', sans-serif; font-size: 13px; margin-bottom: 8px;">
                        📋 BẢNG DỮ LIỆU PAYMENTS GẦN ĐÂY
                    </div>
                    <div class="table-wrapper">
                        <table class="brutal-table" style="font-size: 12px;">
                            <thead>
                                <tr>
                                    <th>Mã GD</th>
                                    <th>Cổng</th>
                                    <th>Số tiền</th>
                                    <th>Trạng thái</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr>
                                    <td><strong>PAY-99180</strong></td>
                                    <td><code>vnpay</code></td>
                                    <td>1.330.000 đ</td>
                                    <td><span class="badge badge-sale">paid</span></td>
                                </tr>
                                <tr>
                                    <td><strong>PAY-99142</strong></td>
                                    <td><code>momo</code></td>
                                    <td>520.000 đ</td>
                                    <td><span class="badge badge-both">refunded</span></td>
                                </tr>
                                <tr>
                                    <td><strong>PAY-99105</strong></td>
                                    <td><code>bank_transfer</code></td>
                                    <td>2.800.000 đ</td>
                                    <td><span class="badge badge-danger">failed</span></td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </div>
            </aside>
        </div>
    </main>
</div>

<jsp:include page="/WEB-INF/views/common/footer.jsp" />
