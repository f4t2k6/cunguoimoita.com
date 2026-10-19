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
<c:set var="assetVer" value="20261020d"/>
<%-- Khách (chưa đăng nhập): link Đăng nhập / Đăng ký mang ?next=trang đang xem để đăng nhập xong quay lại đúng trang.
     loginNext do LoginRequiredFilter đặt. Các nút thao tác (Lưu, Thêm vào giỏ, Nhắn tin...) của khách dẫn tới loginUrl. --%>
<c:set var="isGuest" value="${empty sessionScope.userId}"/>
<c:url var="loginUrl" value="/login"><c:if test="${not empty loginNext}"><c:param name="next" value="${loginNext}"/></c:if></c:url>
<c:url var="registerUrl" value="/register"><c:if test="${not empty loginNext}"><c:param name="next" value="${loginNext}"/></c:if></c:url>
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
  <%-- Khung chat nổi góc phải dưới (footer.jsp) dùng kiểu của chat.css + chat-dock.css --%>
  <c:if test="${sessionScope.userRole == 'customer' and empty hideChatDock}">
    <c:if test="${pageCss != 'chat.css'}"><link rel="stylesheet" href="${ctx}/assets/css/chat.css?v=${assetVer}"></c:if>
    <link rel="stylesheet" href="${ctx}/assets/css/chat-dock.css?v=${assetVer}">
  </c:if>
    <link rel="icon" type="image/png" href="${pageContext.request.contextPath}/assets/images/logo_small.png">
</head>
<body data-ctx="${ctx}">

<header class="topbar">
  <a href="${ctx}/home" class="logo-link"><img src="${ctx}/assets/images/logo.png" alt="Cunguoimoita" class="logo"></a>

  <form class="search-bar" action="${ctx}/feed/search" method="get">
    <img src="${ctx}/assets/images/search-icon.png" alt="" class="search-icon">
    <input type="text" name="q" class="search-input" value="${fn:escapeXml(param.q)}" placeholder="Tìm kiếm trên cunguoimoita...">
  </form>

  <c:choose>
    <c:when test="${isGuest}">
      <%-- Khách: thay nút Thông báo bằng Đăng nhập / Đăng ký --%>
      <div class="guest-auth">
        <a class="btn guest-login" href="${loginUrl}">Đăng nhập</a>
        <a class="btn btn-dark guest-register" href="${registerUrl}">Đăng ký</a>
      </div>
    </c:when>
    <c:otherwise>
    <button type="button" class="notify-btn" id="notifyBtn" aria-haspopup="true" aria-expanded="false" aria-controls="notifyPanel">
      <span>Thông báo</span>
      <img src="${ctx}/assets/images/bell-icon.svg" alt="" class="bell-icon">
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
    </c:otherwise>
  </c:choose>
</header>

<c:set var="isAdmin" value="${sessionScope.userRole == 'admin'}"/>
<nav class="sidebar-menu${isAdmin ? ' admin-menu' : ''}">
<c:choose>
  <%-- Admin: menu trái là các chức năng quản trị (adminTab, adminTodo do AdminBaseServlet đặt; số đỏ = việc đang chờ) --%>
  <c:when test="${isAdmin}">
    <a class="menu-item ${adminTab == 'dashboard' ? 'active' : ''}" href="${ctx}/admin/dashboard">
      <img src="${ctx}/assets/images/admin-dashboard.svg" alt="" class="menu-icon"><span>Tổng quan</span>
    </a>
    <a class="menu-item ${adminTab == 'products' ? 'active' : ''}" href="${ctx}/admin/products">
      <img src="${ctx}/assets/images/admin-review.svg" alt="" class="menu-icon"><span>Duyệt bài</span>
      <c:if test="${adminTodo.pendingProducts > 0}"><span class="count-badge" title="Bài chờ duyệt">${adminTodo.pendingProducts}</span></c:if>
    </a>
    <a class="menu-item ${adminTab == 'reports' ? 'active' : ''}" href="${ctx}/admin/reports">
      <img src="${ctx}/assets/images/admin-report.svg" alt="" class="menu-icon"><span>Báo cáo</span>
      <c:if test="${adminTodo.pendingReports > 0}"><span class="count-badge" title="Báo cáo chờ xử lý">${adminTodo.pendingReports}</span></c:if>
    </a>
    <a class="menu-item ${adminTab == 'disputes' ? 'active' : ''}" href="${ctx}/admin/disputes">
      <img src="${ctx}/assets/images/admin-dispute.svg" alt="" class="menu-icon"><span>Khiếu nại</span>
      <c:if test="${adminTodo.openDisputes > 0}"><span class="count-badge" title="Khiếu nại đang mở">${adminTodo.openDisputes}</span></c:if>
    </a>
    <span class="menu-sep" aria-hidden="true"></span>
    <a class="menu-item ${adminTab == 'users' ? 'active' : ''}" href="${ctx}/admin/users">
      <img src="${ctx}/assets/images/admin-users.svg" alt="" class="menu-icon"><span>Người dùng</span>
    </a>
    <a class="menu-item ${adminTab == 'vouchers' ? 'active' : ''}" href="${ctx}/admin/vouchers">
      <img src="${ctx}/assets/images/admin-voucher.svg" alt="" class="menu-icon"><span>Voucher</span>
    </a>
    <a class="menu-item ${adminTab == 'categories' ? 'active' : ''}" href="${ctx}/admin/categories">
      <img src="${ctx}/assets/images/admin-category.svg" alt="" class="menu-icon"><span>Danh mục</span>
    </a>
    <a class="menu-item ${adminTab == 'carriers' ? 'active' : ''}" href="${ctx}/admin/carriers">
      <img src="${ctx}/assets/images/admin-carrier.svg" alt="" class="menu-icon"><span>Vận chuyển</span>
    </a>
    <a class="menu-item ${adminTab == 'tiers' ? 'active' : ''}" href="${ctx}/admin/tiers">
      <img src="${ctx}/assets/images/admin-tier.svg" alt="" class="menu-icon"><span>Hạng thành viên</span>
    </a>
    <span class="menu-sep" aria-hidden="true"></span>
  </c:when>
  <c:otherwise>
  <%-- Mục đang mở được tô đen: so đường dẫn servlet đã forward tới JSP --%>
  <c:set var="curPath" value="${requestScope['jakarta.servlet.forward.servlet_path']}"/>
  <%-- Bài đăng: về bảng tin (trang chủ) --%>
  <a class="menu-item ${curPath == '/home' ? 'active' : ''}" href="${ctx}/home">
    <img src="${ctx}/assets/images/feed-icon.svg" alt="" class="menu-icon"><span>Bài đăng</span>
  </a>
  <a class="menu-item ${curPath == '/order/cart' ? 'active' : ''}" href="${ctx}/order/cart">
    <img src="${ctx}/assets/images/cart-icon.png" alt="" class="menu-icon"><span>Giỏ hàng</span>
  </a>
  <a class="menu-item ${curPath == '/order/list' and param.as != 'seller' ? 'active' : ''}" href="${ctx}/order/list?as=buyer">
    <img src="${ctx}/assets/images/buy-orders-icon.svg" alt="" class="menu-icon"><span>Đơn mua</span>
  </a>
  <a class="menu-item ${curPath == '/order/list' and param.as == 'seller' ? 'active' : ''}" href="${ctx}/order/list?as=seller">
    <img src="${ctx}/assets/images/sell-orders-icon.svg" alt="" class="menu-icon"><span>Đơn bán</span>
  </a>
  <a class="menu-item ${fn:startsWith(curPath, '/chat/') ? 'active' : ''}" href="${ctx}/chat/inbox">
    <img src="${ctx}/assets/images/message-icon.png" alt="" class="menu-icon"><span>Tin nhắn</span>
    <span class="count-badge" id="chatBadge" hidden></span>
  </a>
  <a class="menu-item ${curPath == '/feed/bucket-list' ? 'active' : ''}" href="${ctx}/feed/bucket-list">
    <img src="${ctx}/assets/images/saved-icon.png" alt="" class="menu-icon"><span>Đã lưu</span>
  </a>
  <a class="menu-item ${curPath == '/product/new' ? 'active' : ''}" href="${ctx}/product/new">
    <img src="${ctx}/assets/images/post-icon.svg" alt="" class="menu-icon">
    <span>Đăng bài</span>
  </a>
  <c:if test="${sessionScope.userRole == 'customer'}">
    <a class="menu-item ${curPath == '/product/mine' ? 'active' : ''}" href="${ctx}/product/mine">
      <img src="${ctx}/assets/images/my-posts-icon.svg" alt="" class="menu-icon">
      <span>Bài của tôi</span>
    </a>
  </c:if>
  </c:otherwise>
</c:choose>
  <c:choose>
    <c:when test="${not empty sessionScope.userId}">
      <%-- Cài đặt: bấm để mở danh sách tài khoản, đơn hàng và nút Đăng xuất (common.js) --%>
      <div class="menu-settings">
        <button type="button" class="menu-item" id="settingsBtn" aria-haspopup="true" aria-expanded="false" aria-controls="settingsPanel">
          <img src="${ctx}/assets/images/settings-icon.svg" alt="" class="menu-icon">
          <span>Cài đặt</span>
        </button>
        <div class="settings-panel" id="settingsPanel" hidden>
          <c:choose>
            <c:when test="${sessionScope.userRole == 'admin'}">
              <a href="${ctx}/admin/dashboard">Trang quản trị</a>
              <a href="${ctx}/home">Xem bảng tin</a>
            </c:when>
            <c:otherwise>
              <a href="${ctx}/account/profile">Hồ sơ cá nhân</a>
              <a href="${ctx}/account/profile#so-dia-chi">Sổ địa chỉ</a>
              <a href="${ctx}/account/profile#doi-mat-khau">Đổi mật khẩu</a>
              <a href="${ctx}/payment/history">Lịch sử thanh toán</a>
              <a href="${ctx}/seller/payouts">Thu nhập bán hàng</a>
              <a href="${ctx}/dispute">Khiếu nại &amp; hoàn tiền</a>
            </c:otherwise>
          </c:choose>
          <form method="post" action="${ctx}/logout">
            <button type="submit" class="settings-logout">Đăng xuất</button>
          </form>
        </div>
      </div>
    </c:when>
    <c:otherwise>
      <a class="menu-item" href="${loginUrl}">
        <img src="${ctx}/assets/images/login-icon.svg" alt="" class="menu-icon">
        <span>Đăng nhập</span>
      </a>
    </c:otherwise>
  </c:choose>
</nav>

<a class="profile-block" href="${isGuest ? loginUrl : ctx.concat(sessionScope.userRole == 'admin' ? '/admin/dashboard' : '/account/profile')}">
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
