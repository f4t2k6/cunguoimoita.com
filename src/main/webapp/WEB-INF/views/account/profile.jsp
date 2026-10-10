<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<jsp:include page="/WEB-INF/views/common/header.jsp" />

<div class="app-layout">
    <jsp:include page="/WEB-INF/views/common/sidebar.jsp" />

    <main class="main-content">
        <section class="brutal-card">
            <div class="section-header" style="margin-bottom: 16px;">
                <div>
                    <span class="badge badge-verified" style="margin-bottom: 6px;">PHÂN HỆ F1 • PACKAGE: org.example.cunguoimoita.account</span>
                    <h1 class="section-title">👤 HỒ SƠ CÁ NHÂN, XÁC THỰC &amp; SỔ ĐỊA CHỈ (USER, CUSTOMER, ADDRESS)</h1>
                    <p class="section-subtitle">
                        Quản lý thông tin lớp <code>Customer extends User</code>, tài khoản nhận giải ngân <code>payoutAccountRef</code> và danh sách <code>Address</code>
                    </p>
                </div>
            </div>

            <!-- CHỈ SỐ UY TÍN CUSTOMER (THEO CLASS DIAGRAM) -->
            <div class="kpi-grid">
                <div class="kpi-card" style="background: #dcfce7;">
                    <div class="kpi-label">Điểm đánh giá (avgRating)</div>
                    <div class="kpi-value">⭐ 4.9 / 5.0</div>
                    <div style="font-size: 12px; color: #065f46;"><code>totalSales = 12</code> • <code>totalSwaps = 6</code></div>
                </div>
                <div class="kpi-card">
                    <div class="kpi-label">Xác thực SĐT (isPhoneVerified)</div>
                    <div class="kpi-value" style="color: #16a34a;">ĐÃ XÁC THỰC</div>
                    <div style="font-size: 12px; color: #4b5563;">SĐT: 0912.345.678 (OTP SMS)</div>
                </div>
                <div class="kpi-card">
                    <div class="kpi-label">Lịch sử bom COD (codRefusalCount)</div>
                    <div class="kpi-value">0 / 2 LẦN</div>
                    <div style="font-size: 12px; color: #16a34a;"><code>canUseCod() = true</code> (Đủ điều kiện COD)</div>
                </div>
                <div class="kpi-card">
                    <div class="kpi-label">Dư nợ phí hoàn (outstandingDebt)</div>
                    <div class="kpi-value">0 đ</div>
                    <div style="font-size: 12px; color: #4b5563;">Không có công nợ vận chuyển</div>
                </div>
            </div>
        </section>

        <div class="split-2col">
            <!-- CỘT TRÁI: CẬP NHẬT HỒ SƠ & TÀI KHOẢN NHẬN TIỀN PAYOUT -->
            <div class="brutal-card">
                <h2 class="section-title" style="font-size: 17px; margin-bottom: 14px;">
                    📝 THÔNG TIN TÀI KHOẢN &amp; NGÂN HÀNG NHẬN TIỀN BÁN HÀNG
                </h2>
                <form action="${pageContext.request.contextPath}/ui" method="get">
                    <input type="hidden" name="page" value="profile" />
                    <div class="form-grid">
                        <div class="form-group">
                            <label class="form-label">Họ và tên (<code>User.fullName</code>)</label>
                            <input type="text" class="form-control" value="Nguyễn Tuấn Anh" />
                        </div>
                        <div class="form-group">
                            <label class="form-label">Email đăng nhập (<code>User.email</code>)</label>
                            <input type="email" class="form-control" value="tuananh.tv4@cunguoimoita.com" />
                        </div>
                        <div class="form-group">
                            <label class="form-label">Số điện thoại (<code>User.phone</code>)</label>
                            <input type="text" class="form-control" value="0912345678" />
                        </div>
                        <div class="form-group">
                            <label class="form-label">Hạng thành viên (<code>Customer.tierId</code>)</label>
                            <input type="text" class="form-control" value="Gold Tier (1,450 EcoPoints)" readonly />
                        </div>
                        <div class="form-group full-width">
                            <label class="form-label">Tài khoản thụ hưởng giải ngân F9 (<code>Customer.payoutAccountRef</code>)</label>
                            <input type="text" class="form-control" value="VIETCOMBANK - 1029384756 - NGUYEN TUAN ANH" />
                        </div>
                    </div>
                    <button type="submit" class="btn btn-primary">💾 Lưu cập nhật hồ sơ (updateProfile)</button>
                </form>

                <!-- SỔ ĐỊA CHỈ GIAO NHẬN (CLASS ADDRESS) -->
                <h3 style="font-family: 'Bungee', sans-serif; font-size: 16px; margin: 24px 0 12px;">
                    📍 SỔ ĐỊA CHỈ GIAO NHẬN (BẢNG ADDRESSES)
                </h3>
                <div style="display: flex; flex-direction: column; gap: 10px;">
                    <div style="background: #dcfce7; border: 1.5px solid #111; border-radius: 12px; padding: 12px; display: flex; justify-content: space-between; align-items: center;">
                        <div>
                            <div style="font-weight: 700;">🏠 Ký túc xá Khu B ĐHQG <span class="badge badge-sale">isDefault = true</span></div>
                            <div style="font-size: 12px; color: #374151;">Đường Tạ Quang Bửu, Phường Đông Hoà, TP. Dĩ An / Thủ Đức, TP.HCM</div>
                        </div>
                        <span class="btn btn-outline btn-sm">Sửa</span>
                    </div>
                    <div style="background: #f4f4f0; border: 1.5px solid #111; border-radius: 12px; padding: 12px; display: flex; justify-content: space-between; align-items: center;">
                        <div>
                            <div style="font-weight: 700;">🏢 Văn phòng CLB Sinh Viên Xanh</div>
                            <div style="font-size: 12px; color: #374151;">Số 1 Võ Văn Ngân, Phường Linh Chiểu, TP. Thủ Đức, TP.HCM</div>
                        </div>
                        <span class="btn btn-outline btn-sm">Đặt mặc định</span>
                    </div>
                </div>
            </div>

            <!-- CỘT PHẢI: KHUNG ĐĂNG NHẬP / ĐĂNG KÝ DÀNH CHO GUEST -->
            <aside class="brutal-card" style="background: #fffef8;">
                <span class="badge badge-swap" style="margin-bottom: 8px;">GUEST.REGISTER() &amp; USER.LOGIN()</span>
                <h2 class="section-title" style="font-size: 17px; margin-bottom: 12px;">
                    🔐 FORM ĐĂNG NHẬP / ĐĂNG KÝ THÀNH VIÊN
                </h2>
                <form action="${pageContext.request.contextPath}/ui" method="get">
                    <input type="hidden" name="page" value="home" />
                    <div class="form-group">
                        <label class="form-label">Email hoặc Số điện thoại</label>
                        <input type="text" class="form-control" placeholder="nhapemail@student.edu.vn" />
                    </div>
                    <div class="form-group">
                        <label class="form-label">Mật khẩu (<code>passwordHash</code> BCrypt)</label>
                        <input type="password" class="form-control" value="••••••••••••" />
                    </div>
                    <div style="display: flex; gap: 10px; margin-top: 12px;">
                        <button type="submit" class="btn btn-dark" style="flex: 1;">Đăng nhập</button>
                        <button type="submit" class="btn btn-amber" style="flex: 1;">Đăng ký mới (+50 EcoPoints)</button>
                    </div>
                </form>
            </aside>
        </div>
    </main>
</div>

<jsp:include page="/WEB-INF/views/common/footer.jsp" />
