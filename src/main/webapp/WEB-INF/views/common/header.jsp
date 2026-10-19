<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%--
  Khung giao diện dùng chung (cả nhóm). Mỗi trang viết:
    <%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
    <c:set var="pageTitle" value="Tên trang"/>
    <c:set var="pageCss" value="chat.css"/>          (tuỳ chọn: CSS riêng của chức năng)
    <%@ include file="/WEB-INF/views/common/header.jsp" %>
      ... nội dung trang ...
    <%@ include file="/WEB-INF/views/common/footer.jsp" %>
--%>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<%-- Số phiên bản gắn vào link CSS/JS: sửa CSS/JS xong thì đổi số này để trình duyệt tải bản mới, không dùng bản cũ đã lưu --%>
<c:set var="assetVer" value="20261019d"/>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>${empty pageTitle ? 'Cunguoimoita' : pageTitle.concat(' - Cunguoimoita')}</title>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Bungee&family=Poppins:wght@300;400;500;600;700&display=swap" rel="stylesheet">
  <link rel="stylesheet" href="${ctx}/assets/css/style.css?v=${assetVer}">
  <c:if test="${not empty pageCss}"><link rel="stylesheet" href="${ctx}/assets/css/${pageCss}?v=${assetVer}"></c:if>
</head>
<body data-ctx="${ctx}">

<header class="topbar">
  <a href="${ctx}/home" class="logo-link"><img src="${ctx}/assets/images/logo.png" alt="Cunguoimoita" class="logo"></a>

  <form class="search-bar" action="${ctx}/feed/search" method="get">
    <img src="${ctx}/assets/images/search-icon.png" alt="" class="search-icon">
    <input type="text" name="q" class="search-input" value="${fn:escapeXml(param.q)}" placeholder="Tìm kiếm trên cunguoimoita...">
  </form>

  <button type="button" class="notify-btn" id="notifyBtn" aria-haspopup="true" aria-expanded="false" aria-controls="notifyPanel">
    <span>Thông báo</span>
    <svg class="bell-icon" viewBox="0 0 48 48" fill="none" stroke="#000" stroke-width="1.6" stroke-linejoin="round" aria-hidden="true">
      <path d="M24 5v4M15 30V20a9 9 0 0 1 18 0v10l5 6H10l5-6z"/>
      <path d="M20 40a4 4 0 0 0 8 0"/>
      <path d="M10 36l4 5h20l4-5"/>
    </svg>
    <span class="count-badge notify-badge" id="notifyBadge" hidden></span>
  </button>

  <%-- Danh sách thông báo xổ xuống khi bấm nút Thông báo (common.js tải qua /notifications/list) --%>
  <div class="notify-panel" id="notifyPanel" hidden>
    <div class="notify-panel-head">
      <strong>Thông báo</strong>
      <button type="button" class="notify-link" id="notifyReadAll">Đánh dấu tất cả đã đọc</button>
    </div>
    <ul class="notify-list" id="notifyList"></ul>
    <a class="notify-all" href="${ctx}/notifications">Xem tất cả</a>
  </div>
</header>

<nav class="sidebar-menu">
  <a class="menu-item" href="${ctx}/order/cart">
    <img src="${ctx}/assets/images/cart-icon.png" alt="" class="menu-icon"><span>Giỏ hàng</span>
  </a>
  <a class="menu-item" href="${ctx}/chat/inbox">
    <img src="${ctx}/assets/images/message-icon.png" alt="" class="menu-icon"><span>Tin nhắn</span>
    <span class="count-badge" id="chatBadge" hidden></span>
  </a>
  <a class="menu-item" href="${ctx}/feed/bucket-list">
    <img src="${ctx}/assets/images/saved-icon.png" alt="" class="menu-icon"><span>Đã lưu</span>
  </a>
  <%-- Đăng bài và quản lý bài (F2): admin không bán hàng nên không thấy --%>
  <c:if test="${sessionScope.userRole != 'admin'}">
    <a class="menu-item" href="${ctx}/product/new">
      <svg class="menu-icon" viewBox="0 0 32 32" fill="none" stroke="#000" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
        <rect x="5" y="5" width="22" height="22" rx="6"/><path d="M16 10.5v11M10.5 16h11"/>
      </svg>
      <span>Đăng bài</span>
    </a>
  </c:if>
  <c:if test="${sessionScope.userRole == 'customer'}">
    <a class="menu-item" href="${ctx}/product/mine">
      <svg class="menu-icon" viewBox="0 0 32 32" fill="none" stroke="#000" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
        <path d="M5 11l11-6 11 6-11 6z"/><path d="M5 11v10l11 6 11-6V11"/><path d="M16 17v10"/>
      </svg>
      <span>Bài của tôi</span>
    </a>
  </c:if>
  <%-- Admin thấy thêm mục Quản trị (F10) --%>
  <c:if test="${sessionScope.userRole == 'admin'}">
    <a class="menu-item" href="${ctx}/admin/dashboard">
      <svg class="menu-icon" viewBox="0 0 32 32" fill="none" stroke="#000" stroke-width="1.6" stroke-linejoin="round" aria-hidden="true">
        <path d="M16 4l10 4v7c0 6.5-4.3 11.3-10 13-5.7-1.7-10-6.5-10-13V8z"/><path d="M11.5 16l3.2 3.2 6-6.4"/>
      </svg>
      <span>Quản trị</span>
    </a>
  </c:if>
  <c:choose>
    <c:when test="${not empty sessionScope.userId}">
      <%-- Cài đặt: bấm để mở danh sách tài khoản, đơn hàng và nút Đăng xuất (common.js) --%>
      <div class="menu-settings">
        <button type="button" class="menu-item" id="settingsBtn" aria-haspopup="true" aria-expanded="false" aria-controls="settingsPanel">
          <svg class="menu-icon" viewBox="0 0 32 32" fill="none" stroke="#000" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <circle cx="16" cy="16" r="4"/>
            <path d="M16 4.5v3M16 24.5v3M4.5 16h3M24.5 16h3M7.9 7.9l2.1 2.1M22 22l2.1 2.1M7.9 24.1L10 22M22 10l2.1-2.1"/>
            <circle cx="16" cy="16" r="8.5"/>
          </svg>
          <span>Cài đặt</span>
        </button>
        <div class="settings-panel" id="settingsPanel" hidden>
          <c:choose>
            <c:when test="${sessionScope.userRole == 'admin'}">
              <a href="${ctx}/admin/dashboard">Trang quản trị</a>
            </c:when>
            <c:otherwise>
              <a href="${ctx}/account/profile">Hồ sơ cá nhân</a>
              <a href="${ctx}/account/profile#so-dia-chi">Sổ địa chỉ</a>
              <a href="${ctx}/account/profile#doi-mat-khau">Đổi mật khẩu</a>
              <a href="${ctx}/order/list?as=buyer">Đơn mua</a>
              <a href="${ctx}/order/list?as=seller">Đơn bán</a>
              <a href="${ctx}/seller/payouts">Thu nhập</a>
              <a href="${ctx}/dispute">Khiếu nại &amp; hoàn tiền</a>
              <a href="${ctx}/review/tier">Hạng thành viên</a>
            </c:otherwise>
          </c:choose>
          <form method="post" action="${ctx}/logout">
            <button type="submit" class="settings-logout">Đăng xuất</button>
          </form>
        </div>
      </div>
    </c:when>
    <c:otherwise>
      <a class="menu-item" href="${ctx}/login">
        <svg class="menu-icon" viewBox="0 0 32 32" fill="none" stroke="#000" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M19 6h6v20h-6"/><path d="M13 10l6 6-6 6"/><path d="M19 16H6"/>
        </svg>
        <span>Đăng nhập</span>
      </a>
    </c:otherwise>
  </c:choose>
</nav>

<a class="profile-block" href="${ctx}${sessionScope.userRole == 'admin' ? '/admin/dashboard' : '/account/profile'}">
  <c:choose>
    <c:when test="${not empty sessionScope.avatarPath}"><img src="${ctx}${sessionScope.avatarPath}" alt="avatar" class="profile-avatar"></c:when>
    <c:otherwise><img src="${ctx}/assets/images/default-avatar.png" alt="avatar" class="profile-avatar"></c:otherwise>
  </c:choose>
  <span class="username-box"><span class="username" id="usernameText"><c:out value="${empty sessionScope.fullName ? 'Khách' : sessionScope.fullName}" /></span></span>
</a>

<main class="page-main ${pageMainClass}">
<%-- Nút quay lại nhỏ đầu mỗi trang (trừ trang chủ, đặt hideBack). Có trang trước trong web thì quay về trang đó,
     không thì tới backUrl (trang đặt <c:set var="backUrl" value="/đường-dẫn"/>), mặc định là trang chủ. --%>
<c:if test="${empty hideBack}">
  <a class="back-link" href="${ctx}${empty backUrl ? '/home' : backUrl}" data-back>
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M19 12H5M11 6l-6 6 6 6"/></svg>
    <span>Quay lại</span>
  </a>
</c:if>
