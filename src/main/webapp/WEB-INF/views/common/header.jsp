<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><c:out value="${not empty pageTitle ? pageTitle : 'Cũ Người Mới Ta — Sàn Mua Bán & Trao Đổi Đồ Cũ Eco'}"/></title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Bungee&family=Poppins:wght@400;500;600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css">
</head>
<body>

<!-- THANH CHUYỂN NHANH 10 CHỨC NĂNG DỰ ÁN (F1 -> F10) -->
<nav class="module-switcher" aria-label="Điều hướng nhanh 10 chức năng">
    <span class="module-switcher-label">⚡ DEMO 10 MODULE:</span>
    <a href="${pageContext.request.contextPath}/ui?page=home"
       class="module-chip ${empty currentPage or currentPage == 'home' ? 'active' : ''}">
        🏠 Trang chủ &amp; F3 Feed
    </a>
    <a href="${pageContext.request.contextPath}/ui?page=profile"
       class="module-chip ${currentPage == 'profile' ? 'active' : ''}">
        👤 F1: Tài khoản &amp; Địa chỉ
    </a>
    <a href="${pageContext.request.contextPath}/ui?page=product-detail"
       class="module-chip ${currentPage == 'product-detail' ? 'active' : ''}">
        📦 F2: Sản phẩm &amp; Đăng tin
    </a>
    <a href="${pageContext.request.contextPath}/ui?page=wishlist"
       class="module-chip ${currentPage == 'wishlist' ? 'active' : ''}">
        💚 F3: Bucket List &amp; Gợi ý Swap
    </a>
    <a href="${pageContext.request.contextPath}/ui?page=orders"
       class="module-chip ${currentPage == 'orders' ? 'active' : ''}">
        🛒 F4: Giỏ hàng &amp; Đơn hàng
    </a>
    <a href="${pageContext.request.contextPath}/ui?page=payment"
       class="module-chip highlight-ta ${currentPage == 'payment' or currentPage == 'payment-result' ? 'active' : ''}">
        💳 F5: Thanh toán Escrow (Tuấn Anh)
    </a>
    <a href="${pageContext.request.contextPath}/ui?page=shipment"
       class="module-chip ${currentPage == 'shipment' ? 'active' : ''}">
        🚚 F6: Vận chuyển &amp; COD
    </a>
    <a href="${pageContext.request.contextPath}/ui?page=messages"
       class="module-chip ${currentPage == 'messages' ? 'active' : ''}">
        💬 F7: Chat &amp; Trả giá
    </a>
    <a href="${pageContext.request.contextPath}/ui?page=rewards"
       class="module-chip ${currentPage == 'rewards' ? 'active' : ''}">
        🌱 F8: Đánh giá &amp; EcoPoints
    </a>
    <a href="${pageContext.request.contextPath}/ui?page=dispute"
       class="module-chip highlight-ta ${currentPage == 'dispute' ? 'active' : ''}">
        ⚖️ F9: Khiếu nại &amp; Hoàn tiền (Tuấn Anh)
    </a>
    <a href="${pageContext.request.contextPath}/ui?page=payout"
       class="module-chip highlight-ta ${currentPage == 'payout' ? 'active' : ''}">
        💰 F9: Giải ngân Payout (Tuấn Anh)
    </a>
    <a href="${pageContext.request.contextPath}/ui?page=admin"
       class="module-chip ${currentPage == 'admin' ? 'active' : ''}">
        🛡️ F10: Admin Trọng tài
    </a>
</nav>

<!-- HEADER CHÍNH -->
<header class="app-header">
    <div class="header-brand">
        <a href="${pageContext.request.contextPath}/ui?page=home">
            <img src="${pageContext.request.contextPath}/assets/images/logo.png" alt="Logo Cũ Người Mới Ta" class="logo">
        </a>
    </div>

    <form class="search-bar" action="${pageContext.request.contextPath}/ui" method="get">
        <input type="hidden" name="page" value="home">
        <img src="${pageContext.request.contextPath}/assets/images/search-icon.png" alt="Search" class="search-icon">
        <input type="text" name="keyword" value="<c:out value='${param.keyword}'/>"
               placeholder="Tìm đồ cũ, sách giáo trình, bàn phím cơ, đồ muốn trao đổi..." class="search-input">
    </form>

    <div class="header-actions">
        <a href="${pageContext.request.contextPath}/ui?page=rewards" class="eco-pill" title="Điểm thưởng sống xanh EcoPoints">
            🌱 <span><c:out value="${not empty currentUserEcoPoints ? currentUserEcoPoints : '1,450'}"/> EcoPoints</span>
        </a>
        <a href="${pageContext.request.contextPath}/ui?page=rewards" class="tier-pill" title="Hạng thành viên hiện tại">
            🏆 <c:out value="${not empty currentUserTier ? currentUserTier : 'GOLD TIER (-3% PHÍ)'}"/>
        </a>
        <a href="${pageContext.request.contextPath}/ui?page=product-detail#post-form" class="btn btn-primary btn-sm">
            + Đăng bán / Đổi đồ
        </a>
    </div>
</header>
