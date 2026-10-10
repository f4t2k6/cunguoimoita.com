<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<jsp:include page="/WEB-INF/views/common/demo-header.jsp" />

<div class="app-layout">
    <jsp:include page="/WEB-INF/views/common/sidebar.jsp" />

    <main class="main-content">
        <!-- GIỎ HÀNG & ÁP MÃ VOUCHER (F4 CART, CARTITEM, VOUCHER) -->
        <section class="brutal-card">
            <div class="section-header">
                <div>
                    <span class="badge badge-verified" style="margin-bottom: 6px;">PHÂN HỆ F4 • PACKAGE: org.example.cunguoimoita.order</span>
                    <h1 class="section-title">🛒 GIỎ HÀNG &amp; QUẢN LÝ VÒNG ĐỜI ĐƠN HÀNG (CART, VOUCHER, ORDER)</h1>
                    <p class="section-subtitle">
                        Thực thi <code>Cart.calculateSubtotal()</code>, kiểm tra <code>Voucher.isValid()</code> và theo dõi 8 trạng thái của <code>Order</code>
                    </p>
                </div>
                <a href="${pageContext.request.contextPath}/ui?page=payment&amp;orderCode=ORD-2026-0889" class="btn btn-primary">
                    💳 Tiến hành Thanh toán Đơn đã chọn (Sang F5)
                </a>
            </div>

            <!-- BẢNG ĐƠN HÀNG 8 TRẠNG THÁI (Order.status) -->
            <div class="table-wrapper">
                <table class="brutal-table">
                    <thead>
                        <tr>
                            <th>Mã Đơn (code)</th>
                            <th>Sản Phẩm (OrderItem)</th>
                            <th>Phương thức TT</th>
                            <th>Tạm tính / Ship / Giảm giá</th>
                            <th>Tổng Cộng (totalAmount)</th>
                            <th>Trạng Thái (OrderStatus)</th>
                            <th>Thao Tác Liên Phân Hệ</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td><strong>ORD-2026-0889</strong></td>
                            <td>⌨️ Bàn phím cơ Keychron K2 Pro</td>
                            <td><code>online (VNPay)</code></td>
                            <td>1.350k + 30k - 50k</td>
                            <td><strong style="color: #16a34a;">1.330.000 đ</strong></td>
                            <td><span class="badge badge-swap">awaiting_payment</span></td>
                            <td>
                                <a href="${pageContext.request.contextPath}/ui?page=payment&amp;orderCode=ORD-2026-0889" class="btn btn-primary btn-sm">
                                    Thanh toán (F5)
                                </a>
                            </td>
                        </tr>
                        <tr>
                            <td><strong>ORD-2026-0895</strong></td>
                            <td>📷 Máy ảnh Film Olympus OM-1</td>
                            <td><code>online (MoMo)</code></td>
                            <td>2.800k + 35k - 35k</td>
                            <td><strong style="color: #16a34a;">2.800.000 đ</strong></td>
                            <td><span class="badge badge-both">shipping</span></td>
                            <td>
                                <a href="${pageContext.request.contextPath}/ui?page=shipment" class="btn btn-outline btn-sm">
                                    Vận đơn (F6)
                                </a>
                            </td>
                        </tr>
                        <tr>
                            <td><strong>ORD-2026-0871</strong></td>
                            <td>🪑 Ghế Công Thái Học Sihoo M57</td>
                            <td><code>online (Bank QR)</code></td>
                            <td>1.950k + 50k - 0k</td>
                            <td><strong style="color: #16a34a;">2.000.000 đ</strong></td>
                            <td><span class="badge badge-sale">delivered</span></td>
                            <td style="display: flex; gap: 6px;">
                                <a href="${pageContext.request.contextPath}/ui?page=rewards" class="btn btn-primary btn-sm">
                                    Nhận hàng &amp; Đánh giá (F8)
                                </a>
                                <a href="${pageContext.request.contextPath}/ui?page=dispute" class="btn btn-danger btn-sm">
                                    Khiếu nại (F9)
                                </a>
                            </td>
                        </tr>
                        <tr>
                            <td><strong>ORD-2026-0840</strong></td>
                            <td>🚲 Xe đạp Touring Giant Escape 3</td>
                            <td><code>online (VNPay)</code></td>
                            <td>3.400k + 0k - 0k</td>
                            <td><strong style="color: #16a34a;">3.400.000 đ</strong></td>
                            <td><span class="badge badge-sale">completed</span></td>
                            <td>
                                <a href="${pageContext.request.contextPath}/ui?page=payout" class="btn btn-amber btn-sm">
                                    Đã giải ngân Payout (F9)
                                </a>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </section>
    </main>
</div>

<jsp:include page="/WEB-INF/views/common/demo-footer.jsp" />
