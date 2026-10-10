<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<jsp:include page="/WEB-INF/views/common/header.jsp" />

<div class="app-layout">
    <jsp:include page="/WEB-INF/views/common/sidebar.jsp" />

    <main class="main-content">
        <section class="brutal-card">
            <div class="section-header">
                <div>
                    <span class="badge badge-verified" style="margin-bottom: 6px;">PHÂN HỆ F7 • PACKAGE: org.example.cunguoimoita.chat &amp; notification</span>
                    <h1 class="section-title">💬 PHÒNG CHAT THƯƠNG LƯỢNG, TRẢ GIÁ (PRICE OFFER) &amp; THÔNG BÁO</h1>
                    <p class="section-subtitle">
                        Thực thi <code>Conversation.sendMessage()</code>, thẻ trả giá <code>PriceOffer.accept() / decline()</code> và <code>Notification.markRead()</code>
                    </p>
                </div>
            </div>

            <div class="split-2col">
                <!-- KHUNG HỘI THOẠI & THẺ TRẢ GIÁ PRICE OFFER -->
                <div style="background: #f4f4f0; border: 2px solid #111; border-radius: 14px; padding: 18px; display: flex; flex-direction: column; gap: 14px;">
                    <div style="border-bottom: 1.5px solid #111; padding-bottom: 10px; display: flex; justify-content: space-between; align-items: center;">
                        <div>
                            <strong>👤 Minh Khoa (Người bán)</strong> • Đang trao đổi về: <em>Bàn phím Keychron K2 Pro</em>
                        </div>
                        <span class="badge badge-sale">Đang trực tuyến</span>
                    </div>

                    <!-- Tin nhắn text -->
                    <div style="background: #fff; border: 1.5px solid #111; border-radius: 12px; padding: 10px 14px; max-width: 78%;">
                        <div style="font-size: 11px; color: #4b5563;">Tuấn Anh (Người mua) • 14:10 • <code>type = text</code></div>
                        <div style="font-size: 13px;">Chào bạn, bàn phím còn đầy đủ phụ kiện keycap puller và dây zin không ạ?</div>
                    </div>

                    <div style="background: #dcfce7; border: 1.5px solid #111; border-radius: 12px; padding: 10px 14px; max-width: 78%; align-self: flex-end;">
                        <div style="font-size: 11px; color: #065f46;">Minh Khoa (Người bán) • 14:12 • <code>type = text</code></div>
                        <div style="font-size: 13px;">Còn đủ fullbox 100% nha bạn ơi, mình mới dùng kỹ ở phòng trọ thôi.</div>
                    </div>

                    <!-- THẺ TRẢ GIÁ TRỰC TIẾP TRONG CHAT (CLASS PRICEOFFER) -->
                    <div style="background: #fef9c3; border: 2px solid #111; border-radius: 14px; padding: 16px; box-shadow: 4px 4px 0 #111;">
                        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px;">
                            <span class="badge badge-swap">🏷️ THẺ ĐỀ NGHỊ GIÁ (<code>Message.type = price_offer</code>)</span>
                            <code>PriceOffer.status = accepted</code>
                        </div>
                        <div style="font-family: 'Bungee', sans-serif; font-size: 20px; color: #111;">
                            GIÁ ĐỀ NGHỊ: 1.300.000 đ <span style="font-size: 13px; color: #4b5563; font-family: 'Poppins', sans-serif;">(Giá gốc: 1.350.000 đ)</span>
                        </div>
                        <div style="display: flex; gap: 8px; margin-top: 10px;">
                            <a href="${pageContext.request.contextPath}/ui?page=payment&amp;orderCode=ORD-2026-0889" class="btn btn-primary btn-sm">
                                ✔ Chấp nhận &amp; Thanh toán ngay (Sang F5)
                            </a>
                            <span class="btn btn-outline btn-sm">✖ Từ chối (decline)</span>
                        </div>
                    </div>

                    <!-- Ô nhập tin nhắn hoặc gửi trả giá mới -->
                    <form action="${pageContext.request.contextPath}/ui" method="get" style="display: flex; gap: 8px; margin-top: 6px;">
                        <input type="hidden" name="page" value="messages" />
                        <input type="text" class="form-control" style="flex: 1;" placeholder="Nhập tin nhắn hoặc mức giá muốn thương lượng..." />
                        <button type="submit" class="btn btn-dark">Gửi tin</button>
                    </form>
                </div>

                <!-- DANH SÁCH THÔNG BÁO HỆ THỐNG (CLASS NOTIFICATION) -->
                <aside class="brutal-card" style="background: #fffef8; padding: 18px;">
                    <h2 style="font-family: 'Bungee', sans-serif; font-size: 16px; margin-top: 0;">
                        🔔 TRUNG TÂM THÔNG BÁO (NOTIFICATION)
                    </h2>
                    <div style="display: flex; flex-direction: column; gap: 10px; font-size: 13px;">
                        <div style="background: #dcfce7; border: 1.5px solid #111; border-radius: 10px; padding: 10px;">
                            <strong>💳 [F5 Payment]</strong> Đơn hàng <code>ORD-2026-0889</code> đã thanh toán thành công 1.330.000 đ qua VNPay.
                        </div>
                        <div style="background: #fef9c3; border: 1.5px solid #111; border-radius: 10px; padding: 10px;">
                            <strong>⚖️ [F9 Dispute]</strong> Khiếu nại <code>#DSP-2026-041</code> đã được Admin tiếp nhận thẩm định.
                        </div>
                        <div style="background: #e0f2fe; border: 1.5px solid #111; border-radius: 10px; padding: 10px;">
                            <strong>💰 [F9 Payout]</strong> Khoản giải ngân <code>3.298.000 đ</code> đã chuyển vào Vietcombank của bạn.
                        </div>
                    </div>
                </aside>
            </div>
        </section>
    </main>
</div>

<jsp:include page="/WEB-INF/views/common/footer.jsp" />
