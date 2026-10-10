<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>

<jsp:include page="/WEB-INF/views/common/demo-header.jsp" />

<div class="app-layout">
    <jsp:include page="/WEB-INF/views/common/sidebar.jsp" />

    <main class="main-content">
        <!-- 9 DANH MỤC GỐC CỦA DỰ ÁN (F2 Category) -->
        <section class="category-bar" aria-label="Danh mục sản phẩm">
            <a href="${pageContext.request.contextPath}/ui?page=home&amp;cat=all"
               class="category-item ${empty param.cat or param.cat == 'all' ? 'active' : ''}">
                <div class="category-icon-box">👕</div>
                <span>Thời trang</span>
            </a>
            <a href="${pageContext.request.contextPath}/ui?page=home&amp;cat=electronics"
               class="category-item ${param.cat == 'electronics' ? 'active' : ''}">
                <div class="category-icon-box">💻</div>
                <span>Điện tử</span>
            </a>
            <a href="${pageContext.request.contextPath}/ui?page=home&amp;cat=books"
               class="category-item ${param.cat == 'books' ? 'active' : ''}">
                <div class="category-icon-box">📚</div>
                <span>Sách &amp; VPP</span>
            </a>
            <a href="${pageContext.request.contextPath}/ui?page=home&amp;cat=furniture"
               class="category-item ${param.cat == 'furniture' ? 'active' : ''}">
                <div class="category-icon-box">🪑</div>
                <span>Nội thất</span>
            </a>
            <a href="${pageContext.request.contextPath}/ui?page=home&amp;cat=appliance"
               class="category-item ${param.cat == 'appliance' ? 'active' : ''}">
                <div class="category-icon-box">🍳</div>
                <span>Gia dụng</span>
            </a>
            <a href="${pageContext.request.contextPath}/ui?page=home&amp;cat=hobby"
               class="category-item ${param.cat == 'hobby' ? 'active' : ''}">
                <div class="category-icon-box">🎮</div>
                <span>Giải trí</span>
            </a>
            <a href="${pageContext.request.contextPath}/ui?page=home&amp;cat=baby"
               class="category-item ${param.cat == 'baby' ? 'active' : ''}">
                <div class="category-icon-box">🍼</div>
                <span>Mẹ &amp; Bé</span>
            </a>
            <a href="${pageContext.request.contextPath}/ui?page=home&amp;cat=vehicle"
               class="category-item ${param.cat == 'vehicle' ? 'active' : ''}">
                <div class="category-icon-box">🚲</div>
                <span>Phương tiện</span>
            </a>
            <a href="${pageContext.request.contextPath}/ui?page=home&amp;cat=free"
               class="category-item ${param.cat == 'free' ? 'active' : ''}">
                <div class="category-icon-box">🎁</div>
                <span>Cho tặng</span>
            </a>
        </section>

        <!-- HERO BANNER: BẢN SẮC SÀN ĐỒ CŨ & SWAP BỀN VỮNG -->
        <section class="hero-banner">
            <div>
                <span class="hero-eyebrow">🌱 RE-COMMERCE &amp; ITEM SWAPPING PLATFORM</span>
                <h1 class="hero-title">MÓN ĐỒ CŨ CỦA BẠN LÀ KHO BÁU MỚI CỦA NGƯỜI KHÁC!</h1>
                <p class="hero-desc">
                    Mua bán, trao đổi (Swap) hoặc vừa bán vừa đổi đồ cũ an toàn tuyệt đối với cơ chế
                    <strong>Tạm giữ tiền Escrow (F5)</strong>, kiểm định đối tác và tích lũy
                    <strong>EcoPoints (F8)</strong> giảm phí hoa hồng cho mỗi giao dịch xanh.
                </p>
                <div class="hero-cta-group">
                    <a href="${pageContext.request.contextPath}/ui?page=product-detail#post-form" class="btn btn-primary">
                        + Đăng món đồ đầu tiên
                    </a>
                    <a href="${pageContext.request.contextPath}/ui?page=payment" class="btn btn-amber">
                        💳 Xem Demo Thanh Toán F5 (Tuấn Anh)
                    </a>
                    <a href="${pageContext.request.contextPath}/ui?page=dispute" class="btn btn-outline">
                        ⚖️ Xem Demo Khiếu Nại &amp; Payout F9
                    </a>
                </div>
            </div>

            <div class="hero-stat-card">
                <div style="display: flex; justify-content: space-between; align-items: center;">
                    <span class="badge badge-verified">🔒 ESCROW TRUST SHIELD</span>
                    <span style="font-size: 12px; font-weight: 700;">Real-time Stats</span>
                </div>
                <div style="border-top: 1.5px dashed #111; padding-top: 10px; display: grid; grid-template-columns: 1fr 1fr; gap: 10px;">
                    <div>
                        <div style="font-size: 11px; color: #4b5563;">Đồ đã tái vòng đời</div>
                        <div style="font-family: 'Bungee', sans-serif; font-size: 20px; color: #16a34a;">12,480+</div>
                    </div>
                    <div>
                        <div style="font-size: 11px; color: #4b5563;">Tỷ lệ giải ngân đúng hạn</div>
                        <div style="font-family: 'Bungee', sans-serif; font-size: 20px; color: #111111;">99.4%</div>
                    </div>
                </div>
                <div style="background: #fef9c3; border: 1.5px solid #111; border-radius: 10px; padding: 8px 12px; font-size: 12px;">
                    💡 <strong>Mẹo Swap:</strong> Thêm món đồ vào <em>Bucket List</em> để hệ thống tự động gợi ý người có nhu cầu trao đổi chéo!
                </div>
            </div>
        </section>

        <!-- BỘ LỌC BẢNG TIN (F3 FEED & FILTER) -->
        <section class="brutal-card">
            <div class="section-header" style="margin-bottom: 12px;">
                <div>
                    <h2 class="section-title">🔥 BẢNG TIN ĐỒ CŨ &amp; KÈO TRAO ĐỔI MỚI NHẤT (F3)</h2>
                    <p class="section-subtitle">
                        Lọc theo hình thức giao dịch (<code>listingType</code>), tình trạng (<code>condition</code>) và kiểm định đối tác (<code>verifiedStatus</code>)
                    </p>
                </div>
                <div style="display: flex; gap: 8px; flex-wrap: wrap;">
                    <a href="${pageContext.request.contextPath}/ui?page=home&amp;type=all"
                       class="btn btn-sm ${empty param.type or param.type == 'all' ? 'btn-dark' : 'btn-outline'}">
                        Tất cả (6)
                    </a>
                    <a href="${pageContext.request.contextPath}/ui?page=home&amp;type=sale"
                       class="btn btn-sm ${param.type == 'sale' ? 'btn-primary' : 'btn-outline'}">
                        🏷️ Chỉ bán (SALE)
                    </a>
                    <a href="${pageContext.request.contextPath}/ui?page=home&amp;type=swap"
                       class="btn btn-sm ${param.type == 'swap' ? 'btn-amber' : 'btn-outline'}">
                        🔄 Chỉ đổi (SWAP)
                    </a>
                    <a href="${pageContext.request.contextPath}/ui?page=home&amp;type=both"
                       class="btn btn-sm ${param.type == 'both' ? 'btn-dark' : 'btn-outline'}">
                        ⚡ Bán hoặc Đổi (BOTH)
                    </a>
                    <a href="${pageContext.request.contextPath}/ui?page=home&amp;verified=true"
                       class="btn btn-sm ${param.verified == 'true' ? 'btn-primary' : 'btn-outline'}">
                        ✔ Đã kiểm định Đối tác
                    </a>
                </div>
            </div>

            <!-- DANH SÁCH SẢN PHẨM (PRODUCT GRID) -->
            <div class="product-grid">
                <!-- Card 1: Bán + Đổi (BOTH) -->
                <article class="product-card">
                    <div class="product-thumb" style="background: linear-gradient(135deg, #d1fae5, #a7f3d0); display: flex; align-items: center; justify-content: center; font-size: 64px;">
                        ⌨️
                        <div class="product-stickers">
                            <span class="badge badge-both">⚡ BÁN &amp; ĐỔI (BOTH)</span>
                            <span class="badge badge-verified">✔ ĐỐI TÁC ĐÃ KIỂM ĐỊNH</span>
                        </div>
                    </div>
                    <div class="product-body">
                        <div style="display: flex; justify-content: space-between; font-size: 11px; color: #4b5563;">
                            <span>Danh mục: <strong>Điện tử</strong></span>
                            <span>Tình trạng: <strong>Cũ (95%)</strong></span>
                        </div>
                        <h3 class="product-title">Bàn phím cơ Keychron K2 Pro RGB Hotswap Red Switch Fullbox</h3>
                        <div class="product-price">1.350.000 đ</div>
                        <div class="swap-note-box">
                            🔄 <strong>Muốn đổi:</strong> Chuột Logitech MX Master 3S hoặc tai nghe Sony bù trừ thương lượng
                        </div>
                        <div class="product-meta">
                            <span>👤 Minh Khoa (⭐ 4.9) • Q.Thủ Đức</span>
                            <span class="badge badge-sale">Có COD</span>
                        </div>
                        <div style="display: flex; gap: 8px; margin-top: 8px;">
                            <a href="${pageContext.request.contextPath}/ui?page=product-detail&amp;id=101" class="btn btn-outline btn-sm" style="flex: 1;">
                                Xem chi tiết
                            </a>
                            <a href="${pageContext.request.contextPath}/ui?page=payment&amp;orderCode=ORD-2026-0101" class="btn btn-primary btn-sm" style="flex: 1;">
                                Mua ngay (F5)
                            </a>
                        </div>
                    </div>
                </article>

                <!-- Card 2: Chỉ bán (SALE) -->
                <article class="product-card">
                    <div class="product-thumb" style="background: linear-gradient(135deg, #e0f2fe, #bae6fd); display: flex; align-items: center; justify-content: center; font-size: 64px;">
                        📷
                        <div class="product-stickers">
                            <span class="badge badge-sale">🏷️ BÁN (SALE)</span>
                            <span class="badge badge-verified">✔ ĐỐI TÁC ĐÃ KIỂM ĐỊNH</span>
                        </div>
                    </div>
                    <div class="product-body">
                        <div style="display: flex; justify-content: space-between; font-size: 11px; color: #4b5563;">
                            <span>Danh mục: <strong>Giải trí</strong></span>
                            <span>Tình trạng: <strong>Cũ (92%)</strong></span>
                        </div>
                        <h3 class="product-title">Máy ảnh Film Olympus OM-1 + Lens Zuiko 50mm f/1.8 đo sáng chuẩn</h3>
                        <div class="product-price">2.800.000 đ</div>
                        <div class="swap-note-box" style="background: #f3f4f6; border-color: #9ca3af; color: #374151;">
                            🔒 Giao dịch qua <strong>Escrow F5</strong>: Giữ tiền an toàn 3 ngày kiểm tra máy
                        </div>
                        <div class="product-meta">
                            <span>👤 Hoàng Nam (⭐ 5.0) • Q.1, TP.HCM</span>
                            <span class="badge badge-pending">Chỉ Online</span>
                        </div>
                        <div style="display: flex; gap: 8px; margin-top: 8px;">
                            <a href="${pageContext.request.contextPath}/ui?page=product-detail&amp;id=102" class="btn btn-outline btn-sm" style="flex: 1;">
                                Xem chi tiết
                            </a>
                            <a href="${pageContext.request.contextPath}/ui?page=payment&amp;orderCode=ORD-2026-0102" class="btn btn-primary btn-sm" style="flex: 1;">
                                Thanh toán F5
                            </a>
                        </div>
                    </div>
                </article>

                <!-- Card 3: Chỉ trao đổi (SWAP) -->
                <article class="product-card">
                    <div class="product-thumb" style="background: linear-gradient(135deg, #fef3c7, #fde68a); display: flex; align-items: center; justify-content: center; font-size: 64px;">
                        📚
                        <div class="product-stickers">
                            <span class="badge badge-swap">🔄 CHỈ ĐỔI (SWAP)</span>
                        </div>
                    </div>
                    <div class="product-body">
                        <div style="display: flex; justify-content: space-between; font-size: 11px; color: #4b5563;">
                            <span>Danh mục: <strong>Sách &amp; VPP</strong></span>
                            <span>Tình trạng: <strong>Mới (99%)</strong></span>
                        </div>
                        <h3 class="product-title">Bộ 4 cuốn Clean Code, Refactoring, Design Patterns &amp; Murach's Servlet JSP</h3>
                        <div class="product-price" style="color: #b45309;">Định giá: ~650.000 đ</div>
                        <div class="swap-note-box">
                            🔄 <strong>Muốn đổi:</strong> Balo đựng vừa Laptop 15.6 inch hoặc giá đỡ màn hình North Bayou
                        </div>
                        <div class="product-meta">
                            <span>👤 Lan Anh (⭐ 4.8) • ĐH Quốc Gia</span>
                            <span class="badge badge-swap">+50 EcoPoints</span>
                        </div>
                        <div style="display: flex; gap: 8px; margin-top: 8px;">
                            <a href="${pageContext.request.contextPath}/ui?page=wishlist" class="btn btn-amber btn-sm" style="flex: 1;">
                                Đề nghị Đổi (F3)
                            </a>
                            <a href="${pageContext.request.contextPath}/ui?page=messages" class="btn btn-outline btn-sm" style="flex: 1;">
                                Nhắn tin (F7)
                            </a>
                        </div>
                    </div>
                </article>

                <!-- Card 4: Nội thất bàn học -->
                <article class="product-card">
                    <div class="product-thumb" style="background: linear-gradient(135deg, #f3e8ff, #e9d5ff); display: flex; align-items: center; justify-content: center; font-size: 64px;">
                        🪑
                        <div class="product-stickers">
                            <span class="badge badge-both">⚡ BÁN &amp; ĐỔI (BOTH)</span>
                        </div>
                    </div>
                    <div class="product-body">
                        <div style="display: flex; justify-content: space-between; font-size: 11px; color: #4b5563;">
                            <span>Danh mục: <strong>Nội thất</strong></span>
                            <span>Tình trạng: <strong>Cũ (90%)</strong></span>
                        </div>
                        <h3 class="product-title">Ghế Công Thái Học Sihoo M57 Full Lưới Có Kê Chân cho sinh viên IT</h3>
                        <div class="product-price">1.950.000 đ</div>
                        <div class="swap-note-box">
                            🔄 <strong>Muốn đổi:</strong> Màn hình rời 24 inch IPS 100Hz còn bảo hành
                        </div>
                        <div class="product-meta">
                            <span>👤 Quốc Bảo (⭐ 4.7) • Q.Bình Thạnh</span>
                            <span class="badge badge-sale">Có COD</span>
                        </div>
                        <div style="display: flex; gap: 8px; margin-top: 8px;">
                            <a href="${pageContext.request.contextPath}/ui?page=orders" class="btn btn-outline btn-sm" style="flex: 1;">
                                + Giỏ hàng (F4)
                            </a>
                            <a href="${pageContext.request.contextPath}/ui?page=payment&amp;orderCode=ORD-2026-0104" class="btn btn-primary btn-sm" style="flex: 1;">
                                Mua ngay (F5)
                            </a>
                        </div>
                    </div>
                </article>

                <!-- Card 5: Thời trang Vintage -->
                <article class="product-card">
                    <div class="product-thumb" style="background: linear-gradient(135deg, #fce7f3, #fbcfe8); display: flex; align-items: center; justify-content: center; font-size: 64px;">
                        🧥
                        <div class="product-stickers">
                            <span class="badge badge-sale">🏷️ BÁN (SALE)</span>
                        </div>
                    </div>
                    <div class="product-body">
                        <div style="display: flex; justify-content: space-between; font-size: 11px; color: #4b5563;">
                            <span>Danh mục: <strong>Thời trang</strong></span>
                            <span>Tình trạng: <strong>Mới (98%)</strong></span>
                        </div>
                        <h3 class="product-title">Áo khoác Denim Levi's Vintage Trucker Jacket Size L chính hãng</h3>
                        <div class="product-price">520.000 đ</div>
                        <div class="swap-note-box" style="background: #dcfce7; border-color: #16a34a; color: #065f46;">
                            🌱 Mua món đồ này giúp giảm ~4.2kg khí thải CO2 thời trang nhanh!
                        </div>
                        <div class="product-meta">
                            <span>👤 Thảo Vy (⭐ 4.9) • Q.3, TP.HCM</span>
                            <span class="badge badge-sale">Có COD</span>
                        </div>
                        <div style="display: flex; gap: 8px; margin-top: 8px;">
                            <a href="${pageContext.request.contextPath}/ui?page=messages" class="btn btn-outline btn-sm" style="flex: 1;">
                                Trả giá (F7)
                            </a>
                            <a href="${pageContext.request.contextPath}/ui?page=payment&amp;orderCode=ORD-2026-0105" class="btn btn-primary btn-sm" style="flex: 1;">
                                Mua ngay (F5)
                            </a>
                        </div>
                    </div>
                </article>

                <!-- Card 6: Phương tiện xanh -->
                <article class="product-card">
                    <div class="product-thumb" style="background: linear-gradient(135deg, #dcfce7, #bbf7d0); display: flex; align-items: center; justify-content: center; font-size: 64px;">
                        🚲
                        <div class="product-stickers">
                            <span class="badge badge-both">⚡ BÁN &amp; ĐỔI (BOTH)</span>
                            <span class="badge badge-verified">✔ ĐỐI TÁC ĐÃ KIỂM ĐỊNH</span>
                        </div>
                    </div>
                    <div class="product-body">
                        <div style="display: flex; justify-content: space-between; font-size: 11px; color: #4b5563;">
                            <span>Danh mục: <strong>Phương tiện</strong></span>
                            <span>Tình trạng: <strong>Cũ (88%)</strong></span>
                        </div>
                        <h3 class="product-title">Xe đạp Touring Giant Escape 3 đi học trong Làng Đại Học siêu nhẹ</h3>
                        <div class="product-price">3.400.000 đ</div>
                        <div class="swap-note-box">
                            🔄 <strong>Muốn đổi:</strong> iPad Gen 9 hoặc máy đọc sách Kindle Oasis 3
                        </div>
                        <div class="product-meta">
                            <span>👤 Tuấn Anh (⭐ 4.9) • Làng ĐH</span>
                            <span class="badge badge-sale">+120 EcoPoints</span>
                        </div>
                        <div style="display: flex; gap: 8px; margin-top: 8px;">
                            <a href="${pageContext.request.contextPath}/ui?page=product-detail&amp;id=106" class="btn btn-outline btn-sm" style="flex: 1;">
                                Xem chi tiết
                            </a>
                            <a href="${pageContext.request.contextPath}/ui?page=payment&amp;orderCode=ORD-2026-0106" class="btn btn-primary btn-sm" style="flex: 1;">
                                Mua ngay (F5)
                            </a>
                        </div>
                    </div>
                </article>
            </div>
        </section>

        <!-- SƠ ĐỒ ĐIỀU HƯỚNG TOÀN BỘ 10 CHỨC NĂNG DỰ ÁN (F1 -> F10) -->
        <section class="brutal-card" style="background: #fffef8;">
            <div class="section-header">
                <div>
                    <h2 class="section-title">🗺️ BẢN ĐỒ GIAO DIỆN 10 CHỨC NĂNG DỰ ÁN (CLASS DIAGRAM MAPPING)</h2>
                    <p class="section-subtitle">
                        Bấm vào bất kỳ phân hệ nào bên dưới để kiểm tra giao diện JSP/JSTL tương ứng của từng thành viên trong nhóm
                    </p>
                </div>
            </div>
            <div class="kpi-grid">
                <a href="${pageContext.request.contextPath}/ui?page=profile" class="kpi-card">
                    <span class="kpi-label">F1 • Account &amp; Address</span>
                    <div class="kpi-value" style="font-size: 17px;">👤 Hồ sơ &amp; Địa chỉ</div>
                    <div style="font-size: 12px; color: #4b5563;">User, Customer, Address, COD Refusal &amp; Payout Ref</div>
                </a>
                <a href="${pageContext.request.contextPath}/ui?page=product-detail" class="kpi-card">
                    <span class="kpi-label">F2 • Product &amp; Category</span>
                    <div class="kpi-value" style="font-size: 17px;">📦 Đăng &amp; Chi tiết SP</div>
                    <div style="font-size: 12px; color: #4b5563;">Sale / Swap / Both, Tình trạng &amp; Kiểm định đối tác</div>
                </a>
                <a href="${pageContext.request.contextPath}/ui?page=wishlist" class="kpi-card">
                    <span class="kpi-label">F3 • Feed &amp; Wishlist</span>
                    <div class="kpi-value" style="font-size: 17px;">💚 Bucket List Swap</div>
                    <div style="font-size: 12px; color: #4b5563;">Lưu sản phẩm yêu thích &amp; Gợi ý ghép đôi trao đổi</div>
                </a>
                <a href="${pageContext.request.contextPath}/ui?page=orders" class="kpi-card">
                    <span class="kpi-label">F4 • Cart &amp; Order</span>
                    <div class="kpi-value" style="font-size: 17px;">🛒 Giỏ hàng &amp; Đơn mua</div>
                    <div style="font-size: 12px; color: #4b5563;">CartItem, Voucher, Order 8 trạng thái chuẩn</div>
                </a>
                <a href="${pageContext.request.contextPath}/ui?page=payment" class="kpi-card" style="background: #fef9c3; border-color: #ca8a04;">
                    <span class="kpi-label" style="color: #854d0e;">F5 ★ TUẤN ANH (TV4)</span>
                    <div class="kpi-value" style="font-size: 17px;">💳 Thanh toán Escrow</div>
                    <div style="font-size: 12px; color: #713f12;">VNPay, MoMo, Bank QR, Chặn COD tự động &amp; Biên lai</div>
                </a>
                <a href="${pageContext.request.contextPath}/ui?page=shipment" class="kpi-card">
                    <span class="kpi-label">F6 • Shipping &amp; COD</span>
                    <div class="kpi-value" style="font-size: 17px;">🚚 Vận đơn &amp; Thu hộ</div>
                    <div style="font-size: 12px; color: #4b5563;">Timeline 5 bước giao hàng &amp; Đối soát tiền COD</div>
                </a>
                <a href="${pageContext.request.contextPath}/ui?page=messages" class="kpi-card">
                    <span class="kpi-label">F7 • Chat &amp; Offer</span>
                    <div class="kpi-value" style="font-size: 17px;">💬 Chat &amp; Trả giá</div>
                    <div style="font-size: 12px; color: #4b5563;">Conversation, PriceOffer trực tiếp trong khung chat</div>
                </a>
                <a href="${pageContext.request.contextPath}/ui?page=rewards" class="kpi-card">
                    <span class="kpi-label">F8 • Review &amp; MemberTier</span>
                    <div class="kpi-value" style="font-size: 17px;">🌱 EcoPoints &amp; Hạng TV</div>
                    <div style="font-size: 12px; color: #4b5563;">Đánh giá 5 sao, Bronze/Silver/Gold/Diamond Tier</div>
                </a>
                <a href="${pageContext.request.contextPath}/ui?page=dispute" class="kpi-card" style="background: #fef9c3; border-color: #ca8a04;">
                    <span class="kpi-label" style="color: #854d0e;">F9 ★ TUẤN ANH (TV4)</span>
                    <div class="kpi-value" style="font-size: 17px;">⚖️ Khiếu nại &amp; Refund</div>
                    <div style="font-size: 12px; color: #713f12;">Mở tranh chấp, Đối chiếu bằng chứng &amp; Hoàn tiền</div>
                </a>
                <a href="${pageContext.request.contextPath}/ui?page=payout" class="kpi-card" style="background: #dcfce7; border-color: #16a34a;">
                    <span class="kpi-label" style="color: #14532d;">F9 ★ TUẤN ANH (TV4)</span>
                    <div class="kpi-value" style="font-size: 17px;">💰 Giải ngân Payout</div>
                    <div style="font-size: 12px; color: #14532d;">Tính phí hoa hồng tự động &amp; Xuất báo cáo CSV</div>
                </a>
            </div>
        </section>
    </main>
</div>

<jsp:include page="/WEB-INF/views/common/demo-footer.jsp" />