<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<jsp:include page="/WEB-INF/views/common/demo-header.jsp" />

<div class="app-layout">
    <jsp:include page="/WEB-INF/views/common/sidebar.jsp" />

    <main class="main-content">
        <section class="brutal-card">
            <div class="section-header">
                <div>
                    <span class="badge badge-verified" style="margin-bottom: 6px;">PHÂN HỆ F6 • PACKAGE: org.example.cunguoimoita.shipping</span>
                    <h1 class="section-title">🚚 THEO DÕI HÀNH TRÌNH VẬN ĐƠN &amp; ĐỐI SOÁT THU HỘ COD (SHIPMENT)</h1>
                    <p class="section-subtitle">
                        Quản lý <code>ShippingCarrier</code>, hành trình <code>Shipment</code> (5 trạng thái) và đối soát tiền thu hộ <code>codStatus</code>
                    </p>
                </div>
                <span class="badge badge-both">Tracking: GHN-84920194VN</span>
            </div>

            <!-- TIMELINE 5 BƯỚC SHIPMENT STATUS -->
            <div class="step-timeline">
                <div class="step-node done">
                    <div style="font-size: 11px;">10/10 08:30</div>
                    <div style="font-weight: 700;">Tạo Vận Đơn</div>
                    <code>status: created</code>
                </div>
                <div class="step-node done">
                    <div style="font-size: 11px;">10/10 14:15</div>
                    <div style="font-weight: 700;">Đã Lấy Hàng</div>
                    <code>status: picked_up</code>
                </div>
                <div class="step-node done">
                    <div style="font-size: 11px;">11/10 06:00</div>
                    <div style="font-weight: 700;">Đang Luân Chuyển</div>
                    <code>status: in_transit</code>
                </div>
                <div class="step-node active">
                    <div style="font-size: 11px;">11/10 15:40</div>
                    <div style="font-weight: 700;">Giao Thành Công</div>
                    <code>status: delivered</code>
                </div>
                <div class="step-node">
                    <div style="font-size: 11px;">Nếu bom hàng</div>
                    <div style="font-weight: 700;">Hoàn Trả Hàng</div>
                    <code>status: returned</code>
                </div>
            </div>

            <!-- BẢNG ĐỐI SOÁT VẬN ĐƠN & THU HỘ COD -->
            <div class="table-wrapper">
                <table class="brutal-table">
                    <thead>
                        <tr>
                            <th>Mã Vận Đơn</th>
                            <th>Đơn vị VC (Carrier)</th>
                            <th>Đơn Hàng</th>
                            <th>Phí Ship</th>
                            <th>Tiền Thu Hộ (codAmount)</th>
                            <th>Trạng Thái COD (codStatus)</th>
                            <th>Trạng Thái Giao Hàng</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td><strong>GHN-84920194VN</strong></td>
                            <td>Giao Hàng Nhanh (GHN)</td>
                            <td><code>ORD-2026-0889</code></td>
                            <td>30.000 đ</td>
                            <td>0 đ (Đã TT Online F5)</td>
                            <td><span class="badge badge-pending">none</span></td>
                            <td><span class="badge badge-sale">delivered</span></td>
                        </tr>
                        <tr>
                            <td><strong>GHTK-55109283</strong></td>
                            <td>Giao Hàng Tiết Kiệm</td>
                            <td><code>ORD-2026-0860</code></td>
                            <td>25.000 đ</td>
                            <td><strong>520.000 đ</strong></td>
                            <td><span class="badge badge-sale">remitted (Đã chuyển về sàn)</span></td>
                            <td><span class="badge badge-sale">delivered</span></td>
                        </tr>
                        <tr>
                            <td><strong>VTP-33019284</strong></td>
                            <td>Viettel Post</td>
                            <td><code>ORD-2026-0812</code></td>
                            <td>35.000 đ</td>
                            <td><strong>890.000 đ</strong></td>
                            <td><span class="badge badge-danger">Khách từ chối (+1 codRefusalCount)</span></td>
                            <td><span class="badge badge-danger">returned</span></td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </section>
    </main>
</div>

<jsp:include page="/WEB-INF/views/common/demo-footer.jsp" />
