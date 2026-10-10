<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<aside class="sidebar-panel">
    <nav class="sidebar-menu">
        <div class="sidebar-section-title">KHÁM PHÁ &amp; MUA SẮM</div>

        <a href="${pageContext.request.contextPath}/ui?page=home"
           class="menu-item ${empty currentPage or currentPage == 'home' ? 'active' : ''}">
            <span class="menu-item-left">
                <img src="${pageContext.request.contextPath}/assets/images/search-icon.png" alt="" class="menu-icon">
                <span>Chợ Đồ Cũ &amp; Swap</span>
            </span>
            <span class="menu-badge">F3</span>
        </a>

        <a href="${pageContext.request.contextPath}/ui?page=product-detail"
           class="menu-item ${currentPage == 'product-detail' ? 'active' : ''}">
            <span class="menu-item-left">
                <img src="${pageContext.request.contextPath}/assets/images/group-icon.png" alt="" class="menu-icon">
                <span>Chi tiết &amp; Đăng SP</span>
            </span>
            <span class="menu-badge">F2</span>
        </a>

        <a href="${pageContext.request.contextPath}/ui?page=wishlist"
           class="menu-item ${currentPage == 'wishlist' ? 'active' : ''}">
            <span class="menu-item-left">
                <img src="${pageContext.request.contextPath}/assets/images/saved-icon.png" alt="" class="menu-icon">
                <span>Bucket List &amp; Swap</span>
            </span>
            <span class="menu-badge">F3</span>
        </a>

        <a href="${pageContext.request.contextPath}/ui?page=orders"
           class="menu-item ${currentPage == 'orders' ? 'active' : ''}">
            <span class="menu-item-left">
                <img src="${pageContext.request.contextPath}/assets/images/cart-icon.png" alt="" class="menu-icon">
                <span>Giỏ hàng &amp; Đơn mua</span>
            </span>
            <span class="menu-badge">F4</span>
        </a>

        <a href="${pageContext.request.contextPath}/ui?page=messages"
           class="menu-item ${currentPage == 'messages' ? 'active' : ''}">
            <span class="menu-item-left">
                <img src="${pageContext.request.contextPath}/assets/images/message-icon.png" alt="" class="menu-icon">
                <span>Tin nhắn &amp; Trả giá</span>
            </span>
            <span class="menu-badge">F7</span>
        </a>

        <div class="sidebar-section-title">THANH TOÁN &amp; BẢO VỆ (TV4)</div>

        <a href="${pageContext.request.contextPath}/ui?page=payment"
           class="menu-item ta-task ${currentPage == 'payment' or currentPage == 'payment-result' ? 'active' : ''}">
            <span class="menu-item-left">
                <span>💳</span>
                <span>Cổng Thanh Toán</span>
            </span>
            <span class="menu-badge ta">F5 ★</span>
        </a>

        <a href="${pageContext.request.contextPath}/ui?page=dispute"
           class="menu-item ta-task ${currentPage == 'dispute' ? 'active' : ''}">
            <span class="menu-item-left">
                <span>⚖️</span>
                <span>Khiếu Nại &amp; Refund</span>
            </span>
            <span class="menu-badge ta">F9 ★</span>
        </a>

        <a href="${pageContext.request.contextPath}/ui?page=payout"
           class="menu-item ta-task ${currentPage == 'payout' ? 'active' : ''}">
            <span class="menu-item-left">
                <span>💰</span>
                <span>Ví Bán &amp; Payout</span>
            </span>
            <span class="menu-badge ta">F9 ★</span>
        </a>

        <div class="sidebar-section-title">HỆ THỐNG &amp; HẬU CẦN</div>

        <a href="${pageContext.request.contextPath}/ui?page=shipment"
           class="menu-item ${currentPage == 'shipment' ? 'active' : ''}">
            <span class="menu-item-left">
                <span>🚚</span>
                <span>Vận đơn &amp; Thu hộ COD</span>
            </span>
            <span class="menu-badge">F6</span>
        </a>

        <a href="${pageContext.request.contextPath}/ui?page=rewards"
           class="menu-item ${currentPage == 'rewards' ? 'active' : ''}">
            <span class="menu-item-left">
                <span>🌱</span>
                <span>Đánh giá &amp; Hạng TV</span>
            </span>
            <span class="menu-badge">F8</span>
        </a>

        <a href="${pageContext.request.contextPath}/ui?page=profile"
           class="menu-item ${currentPage == 'profile' ? 'active' : ''}">
            <span class="menu-item-left">
                <span>👤</span>
                <span>Hồ sơ &amp; Sổ địa chỉ</span>
            </span>
            <span class="menu-badge">F1</span>
        </a>

        <a href="${pageContext.request.contextPath}/ui?page=admin"
           class="menu-item ${currentPage == 'admin' ? 'active' : ''}">
            <span class="menu-item-left">
                <span>🛡️</span>
                <span>Admin &amp; Trọng tài</span>
            </span>
            <span class="menu-badge">F10</span>
        </a>
    </nav>

    <a href="${pageContext.request.contextPath}/ui?page=profile" class="profile-block">
        <img src="${pageContext.request.contextPath}/assets/images/avt.png" alt="Avatar" class="profile-avatar">
        <div class="username-box">
            <span class="username">TUẤN ANH (TV4)</span>
            <span class="user-subtext">Uy tín: ⭐ 4.9 • 18 Giao dịch</span>
        </div>
    </a>
</aside>
