<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<jsp:include page="/WEB-INF/views/common/demo-header.jsp" />

<div class="app-layout">
    <jsp:include page="/WEB-INF/views/common/sidebar.jsp" />

    <main class="main-content">
        <!-- CHI TIẾT SẢN PHẨM (F2 PRODUCT) -->
        <section class="brutal-card">
            <div class="section-header">
                <div>
                    <span class="badge badge-verified" style="margin-bottom: 6px;">PHÂN HỆ F2 • PACKAGE: org.example.cunguoimoita.product</span>
                    <h1 class="section-title">⌨️ BÀN PHÍM CƠ KEYCHRON K2 PRO RGB HOTSWAP RED SWITCH FULLBOX</h1>
                </div>
                <div style="display: flex; gap: 8px;">
                    <span class="badge badge-both">listingType = both</span>
                    <span class="badge badge-verified">verified_by_partner</span>
                    <span class="badge badge-sale">status = available</span>
                </div>
            </div>

            <div class="split-2col">
                <div style="background: linear-gradient(135deg, #d1fae5, #fef9c3); border: 2px solid #111; border-radius: 16px; min-height: 280px; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 24px; box-shadow: 4px 4px 0 #111;">
                    <div style="font-size: 88px;">⌨️</div>
                    <div style="font-weight: 700; margin-top: 8px;">Ảnh thực tế đã kiểm định bởi Đối tác Kỹ thuật</div>
                </div>

                <div style="display: flex; flex-direction: column; gap: 12px;">
                    <div style="display: flex; justify-content: space-between; align-items: baseline;">
                        <div>
                            <div style="font-size: 12px; color: #4b5563;">Giá bán đứt (<code>price</code>):</div>
                            <div style="font-family: 'Bungee', sans-serif; font-size: 26px; color: #16a34a;">1.350.000 đ</div>
                        </div>
                        <div style="text-align: right;">
                            <div style="font-size: 12px; color: #4b5563;">Giá trị ước tính đổi đồ (<code>estimatedValue</code>):</div>
                            <div style="font-family: 'Bungee', sans-serif; font-size: 18px; color: #b45309;">~1.400.000 đ</div>
                        </div>
                    </div>

                    <div class="swap-note-box">
                        🔄 <strong>Ghi chú món muốn đổi (<code>desiredExchangeNote</code>):</strong><br/>
                        Ưu tiên đổi ngang hoặc bù trừ hợp lý với Chuột Logitech MX Master 3S, Tai nghe chống ồn Sony WH-1000XM4 hoặc ổ cứng SSD di động 1TB.
                    </div>

                    <div style="font-size: 13px; background: #f4f4f0; border: 1.5px solid #111; border-radius: 10px; padding: 12px;">
                        <div>• <strong>Tình trạng (<code>condition</code>):</strong> <code>cũ</code> (Mới 95%, đầy đủ hộp, dây cáp zin và keycap puller)</div>
                        <div>• <strong>Chấp nhận COD (<code>acceptCod</code>):</strong> <code>true</code> (Có hỗ trợ giao thu hộ)</div>
                        <div>• <strong>Người đăng bán (<code>sellerId</code>):</strong> Minh Khoa (⭐ 4.9 • 22 giao dịch thành công)</div>
                    </div>

                    <div style="display: flex; gap: 10px; flex-wrap: wrap; margin-top: 6px;">
                        <a href="${pageContext.request.contextPath}/ui?page=payment&amp;orderCode=ORD-2026-0889" class="btn btn-primary" style="flex: 1;">
                            💳 Mua ngay qua Escrow (F5)
                        </a>
                        <a href="${pageContext.request.contextPath}/ui?page=messages" class="btn btn-amber" style="flex: 1;">
                            💬 Thương lượng Giá / Đề nghị Swap (F7)
                        </a>
                        <a href="${pageContext.request.contextPath}/ui?page=wishlist" class="btn btn-outline">
                            💚 Lưu Bucket List
                        </a>
                    </div>
                </div>
            </div>
        </section>

        <!-- FORM ĐĂNG TIN SẢN PHẨM MỚI (Product.publish()) -->
        <section id="post-form" class="brutal-card" style="background: #fffef8;">
            <h2 class="section-title" style="font-size: 18px; margin-bottom: 6px;">
                ➕ ĐĂNG BÁN HOẶC ĐĂNG TRAO ĐỔI MÓN ĐỒ CŨ MỚI (<code>Product.publish()</code>)
            </h2>
            <p class="section-subtitle" style="margin-bottom: 16px;">
                Hỗ trợ đầy đủ thuộc tính của thực thể <code>Product</code> trong Class Diagram
            </p>

            <form action="${pageContext.request.contextPath}/ui" method="get">
                <input type="hidden" name="page" value="home" />
                <div class="form-grid">
                    <div class="form-group">
                        <label class="form-label">Tiêu đề sản phẩm (<code>title</code>)</label>
                        <input type="text" class="form-control" placeholder="VD: Máy đọc sách Kindle Paperwhite Gen 5 16GB..." />
                    </div>
                    <div class="form-group">
                        <label class="form-label">Danh mục (<code>categoryId</code>)</label>
                        <select class="form-control">
                            <option>Điện tử &amp; Công nghệ</option>
                            <option>Sách &amp; Văn phòng phẩm</option>
                            <option>Thời trang &amp; Phụ kiện</option>
                            <option>Nội thất &amp; Gia dụng</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label class="form-label">Hình thức giao dịch (<code>listingType</code>)</label>
                        <select class="form-control">
                            <option value="both">both — Vừa Bán Vừa Nhận Đổi Đồ</option>
                            <option value="sale">sale — Chỉ Bán lấy tiền</option>
                            <option value="swap">swap — Chỉ Trao Đổi (Swap)</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label class="form-label">Tình trạng món đồ (<code>condition</code>)</label>
                        <select class="form-control">
                            <option value="mới">mới — Chưa qua sử dụng / Like new 99%</option>
                            <option value="cũ">cũ — Đã qua sử dụng, hoạt động tốt</option>
                            <option value="hỏng nhẹ">hỏng nhẹ — Có lỗi nhỏ đã mô tả rõ</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label class="form-label">Giá bán mong muốn (<code>price</code>)</label>
                        <input type="number" class="form-control" value="1350000" />
                    </div>
                    <div class="form-group">
                        <label class="form-label">Món đồ mong muốn đổi (<code>desiredExchangeNote</code>)</label>
                        <input type="text" class="form-control" placeholder="Nhập món bạn muốn đổi nếu chọn Swap/Both..." />
                    </div>
                    <div class="form-group full-width">
                        <label class="form-label">Mô tả chi tiết tình trạng &amp; lịch sử sử dụng (<code>description</code>)</label>
                        <textarea rows="3" class="form-control" placeholder="Cam kết mô tả trung thực để tránh phát sinh Khiếu nại F9..."></textarea>
                    </div>
                </div>
                <button type="submit" class="btn btn-primary">🚀 Đăng tin lên Bảng tin (publish)</button>
            </form>
        </section>
    </main>
</div>

<jsp:include page="/WEB-INF/views/common/demo-footer.jsp" />
