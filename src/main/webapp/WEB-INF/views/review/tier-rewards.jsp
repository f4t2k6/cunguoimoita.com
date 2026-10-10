<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<jsp:include page="/WEB-INF/views/common/demo-header.jsp" />

<div class="app-layout">
    <jsp:include page="/WEB-INF/views/common/sidebar.jsp" />

    <main class="main-content">
        <!-- HẠNG THÀNH VIÊN & ECOPOINTS (F8 MEMBERTIER) -->
        <section class="brutal-card">
            <div class="section-header">
                <div>
                    <span class="badge badge-verified" style="margin-bottom: 6px;">PHÂN HỆ F8 • PACKAGE: org.example.cunguoimoita.review</span>
                    <h1 class="section-title">🌱 ĐÁNH GIÁ UY TÍN (REVIEW) &amp; HẠNG THÀNH VIÊN XANH (MEMBER TIER)</h1>
                    <p class="section-subtitle">
                        Tích luỹ <code>ecoPoints</code> sau mỗi đơn hàng hoàn tất để nâng hạng <code>MemberTier</code> và giảm phí hoa hồng khi giải ngân <code>Payout (F9)</code>
                    </p>
                </div>
                <span class="tier-pill">🏆 HIỆN TẠI: GOLD TIER (1,450 ECOPOINTS)</span>
            </div>

            <!-- 4 THẺ HẠNG THÀNH VIÊN (BRONZE, SILVER, GOLD, DIAMOND) -->
            <div class="kpi-grid" style="margin-bottom: 22px;">
                <div class="kpi-card">
                    <div class="kpi-label">🥉 BRONZE TIER</div>
                    <div class="kpi-value" style="font-size: 18px;">0+ EcoPoints</div>
                    <div style="font-size: 12px; color: #4b5563;"><code>discountRate = 0%</code> • Phí hoa hồng sàn: <strong>5.0%</strong></div>
                </div>
                <div class="kpi-card">
                    <div class="kpi-label">🥈 SILVER TIER</div>
                    <div class="kpi-value" style="font-size: 18px;">500+ EcoPoints</div>
                    <div style="font-size: 12px; color: #4b5563;"><code>discountRate = 20%</code> • Phí hoa hồng sàn: <strong>4.0%</strong></div>
                </div>
                <div class="kpi-card" style="background: #fef9c3; border-color: #ca8a04;">
                    <div class="kpi-label" style="color: #854d0e;">🥇 GOLD TIER (BẠN ĐANG Ở ĐÂY)</div>
                    <div class="kpi-value" style="font-size: 18px; color: #854d0e;">1,200+ EcoPoints</div>
                    <div style="font-size: 12px; color: #713f12;"><code>discountRate = 40%</code> • Phí hoa hồng sàn chỉ còn: <strong>3.0%</strong></div>
                </div>
                <div class="kpi-card" style="background: #e0f2fe;">
                    <div class="kpi-label" style="color: #0369a1;">💎 DIAMOND TIER</div>
                    <div class="kpi-value" style="font-size: 18px; color: #0c4a6e;">3,000+ EcoPoints</div>
                    <div style="font-size: 12px; color: #075985;"><code>discountRate = 70%</code> • Phí hoa hồng sàn chỉ còn: <strong>1.5%</strong></div>
                </div>
            </div>

            <!-- FORM GỬI ĐÁNH GIÁ 5 SAO SAU GIAO DỊCH (Review.submitReview()) -->
            <div class="split-equal">
                <div style="background: #f4f4f0; border: 1.5px solid #111; border-radius: 14px; padding: 18px;">
                    <h2 style="font-family: 'Bungee', sans-serif; font-size: 16px; margin-top: 0;">
                        ⭐ ĐÁNH GIÁ ĐƠN HÀNG ĐÃ NHẬN (<code>Review.submitReview()</code>)
                    </h2>
                    <form action="${pageContext.request.contextPath}/ui" method="get">
                        <input type="hidden" name="page" value="rewards" />
                        <div class="form-group">
                            <label class="form-label">Chọn số sao (<code>rating: 1..5</code>)</label>
                            <select class="form-control">
                                <option value="5">⭐⭐⭐⭐⭐ (5 sao - Tuyệt vời, đúng mô tả 100%)</option>
                                <option value="4">⭐⭐⭐⭐ (4 sao - Tốt, đóng gói cẩn thận)</option>
                                <option value="3">⭐⭐⭐ (3 sao - Tạm ổn)</option>
                            </select>
                        </div>
                        <div class="form-group">
                            <label class="form-label">Nhận xét thực tế (<code>content</code>)</label>
                            <textarea rows="3" class="form-control" placeholder="Chia sẻ trải nghiệm mua hoặc đổi đồ của bạn..."></textarea>
                        </div>
                        <button type="submit" class="btn btn-primary">Gửi đánh giá &amp; Nhận +25 EcoPoints</button>
                    </form>
                </div>

                <div style="background: #dcfce7; border: 1.5px solid #111; border-radius: 14px; padding: 18px;">
                    <h2 style="font-family: 'Bungee', sans-serif; font-size: 16px; margin-top: 0;">
                        🎁 ĐỔI ECOPOINTS LẤY VOUCHER GIẢM GIÁ (F4 + F8)
                    </h2>
                    <div style="display: flex; flex-direction: column; gap: 10px;">
                        <div class="brutal-card" style="padding: 12px; display: flex; justify-content: space-between; align-items: center;">
                            <div>
                                <strong>Mã <code>ECO50K</code> — Giảm 50.000 đ</strong>
                                <div style="font-size: 12px; color: #4b5563;">Đơn tối thiểu 500.000 đ • Cần 200 EcoPoints</div>
                            </div>
                            <a href="${pageContext.request.contextPath}/ui?page=payment" class="btn btn-primary btn-sm">Áp dụng ở F5</a>
                        </div>
                        <div class="brutal-card" style="padding: 12px; display: flex; justify-content: space-between; align-items: center;">
                            <div>
                                <strong>Mã <code>FREESHIP30</code> — Miễn phí vận chuyển 30.000 đ</strong>
                                <div style="font-size: 12px; color: #4b5563;">Áp dụng mọi đơn GHN/GHTK • Cần 120 EcoPoints</div>
                            </div>
                            <a href="${pageContext.request.contextPath}/ui?page=payment" class="btn btn-amber btn-sm">Đổi ngay</a>
                        </div>
                    </div>
                </div>
            </div>
        </section>
    </main>
</div>

<jsp:include page="/WEB-INF/views/common/demo-footer.jsp" />
