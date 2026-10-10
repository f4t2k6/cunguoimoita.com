<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<jsp:include page="/WEB-INF/views/common/demo-header.jsp" />

<div class="app-layout">
    <jsp:include page="/WEB-INF/views/common/sidebar.jsp" />

    <main class="main-content">
        <section class="brutal-card">
            <div class="section-header">
                <div>
                    <span class="badge badge-verified" style="margin-bottom: 6px;">PHÂN HỆ F3 • PACKAGE: org.example.cunguoimoita.feed</span>
                    <h1 class="section-title">💚 BUCKET LIST YÊU THÍCH &amp; GỢI Ý GHÉP ĐÔI TRAO ĐỔI (WISHLIST)</h1>
                    <p class="section-subtitle">
                        Thực thi phương thức <code>Wishlist.addProduct()</code>, <code>removeProduct()</code> và thuật toán <code>Wishlist.suggestMatches()</code>
                    </p>
                </div>
            </div>

            <div class="split-equal">
                <!-- CỘT 1: MÓN ĐỒ BẠN ĐANG LƯU TRONG WISHLIST -->
                <div style="background: #f4f4f0; border: 2px solid #111; border-radius: 14px; padding: 18px;">
                    <h2 style="font-family: 'Bungee', sans-serif; font-size: 16px; margin-top: 0;">
                        📌 DANH SÁCH ĐÃ LƯU CỦA BẠN (3 MÓN)
                    </h2>
                    <div style="display: flex; flex-direction: column; gap: 12px;">
                        <div class="brutal-card" style="padding: 14px;">
                            <div style="display: flex; justify-content: space-between;">
                                <strong>🎧 Tai nghe Sony WH-1000XM4 Cũ 95%</strong>
                                <span class="badge badge-both">BOTH</span>
                            </div>
                            <div style="font-size: 13px; color: #16a34a; font-weight: 700; margin: 4px 0;">2.950.000 đ</div>
                            <div style="font-size: 12px; color: #4b5563;">Chủ sở hữu đang tìm đổi: <em>Xe đạp thể thao hoặc Bàn phím cơ</em></div>
                        </div>

                        <div class="brutal-card" style="padding: 14px;">
                            <div style="display: flex; justify-content: space-between;">
                                <strong>📚 Bộ sách giáo trình CNTT &amp; Murach's JSP</strong>
                                <span class="badge badge-swap">SWAP</span>
                            </div>
                            <div style="font-size: 13px; color: #b45309; font-weight: 700; margin: 4px 0;">Định giá: ~650.000 đ</div>
                            <div style="font-size: 12px; color: #4b5563;">Chủ sở hữu đang tìm đổi: <em>Balo Laptop 15.6 inch</em></div>
                        </div>
                    </div>
                </div>

                <!-- CỘT 2: KẾT QUẢ GỢI Ý TRAO ĐỔI CHÉO (Wishlist.suggestMatches()) -->
                <div style="background: #fef9c3; border: 2px solid #111; border-radius: 14px; padding: 18px;">
                    <h2 style="font-family: 'Bungee', sans-serif; font-size: 16px; margin-top: 0; color: #854d0e;">
                        ⚡ KÈO GHÉP ĐÔI TỰ ĐỘNG (<code>suggestMatches()</code>)
                    </h2>
                    <div class="brutal-card" style="padding: 16px; margin-bottom: 12px;">
                        <span class="badge badge-sale">ĐỘ KHỚP NHU CẦU: 96%</span>
                        <h3 style="margin: 8px 0 4px; font-size: 15px;">
                            🔄 Đổi <strong>Xe đạp Giant Escape 3</strong> của bạn ↔ lấy <strong>Tai nghe Sony WH-1000XM4</strong> + bù 450.000 đ
                        </h3>
                        <p style="font-size: 12px; color: #4b5563; margin-bottom: 12px;">
                            Khoản tiền bù chênh lệch 450.000 đ sẽ được bảo vệ qua <strong>Cổng Thanh Toán Escrow (F5)</strong>.
                        </p>
                        <div style="display: flex; gap: 8px;">
                            <a href="${pageContext.request.contextPath}/ui?page=messages" class="btn btn-primary btn-sm">
                                💬 Mở phòng Chat thương lượng (F7)
                            </a>
                            <a href="${pageContext.request.contextPath}/ui?page=payment&amp;orderCode=SWAP-2026-019" class="btn btn-amber btn-sm">
                                💳 Đặt cọc tiền bù (F5)
                            </a>
                        </div>
                    </div>
                </div>
            </div>
        </section>
    </main>
</div>

<jsp:include page="/WEB-INF/views/common/demo-footer.jsp" />
