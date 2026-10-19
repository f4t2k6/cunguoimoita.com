<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN" scope="request"/>
<c:set var="pageTitle" value="Quản trị"/>
<c:set var="pageCss" value="admin.css"/>
<c:set var="pageMainClass" value="page-wide"/>
<%@ include file="/WEB-INF/views/common/header.jsp" %>
<div class="admin-page">
  <%@ include file="/WEB-INF/views/admin/_menu.jsp" %>
  <h1>Tổng quan</h1>

  <%-- Việc đang chờ admin --%>
  <div class="admin-stats">
    <a class="admin-stat todo" href="${ctx}/admin/products">
      <strong>${stats.pendingProducts}</strong><span>Bài chờ duyệt</span>
    </a>
    <a class="admin-stat todo" href="${ctx}/admin/reports">
      <strong>${stats.pendingReports}</strong><span>Báo cáo chờ xử lý</span>
    </a>
    <a class="admin-stat todo" href="${ctx}/admin/disputes">
      <strong>${stats.openDisputes}</strong><span>Khiếu nại đang mở</span>
    </a>
  </div>

  <%-- Việc mới nhất ở từng mục, bấm vào là tới đúng chỗ xử lý --%>
  <c:if test="${stats.pendingProducts + stats.pendingReports + stats.openDisputes > 0}">
    <div class="admin-todo">
      <c:if test="${not empty overview.latestPending}">
        <section class="admin-card">
          <header><h2>Bài chờ duyệt</h2><a class="admin-more" href="${ctx}/admin/products">Xem tất cả ${stats.pendingProducts} →</a></header>
          <ul class="admin-list">
            <c:forEach var="p" items="${overview.latestPending}">
              <c:url var="findUrl" value="/admin/products"><c:param name="q" value="${p.name}"/></c:url>
              <li><a href="${findUrl}"><c:out value="${p.name}"/></a>
                <small><c:out value="${p.sellerName}"/> · ${p.postedDate}</small></li>
            </c:forEach>
          </ul>
        </section>
      </c:if>
      <c:if test="${not empty overview.latestReports}">
        <section class="admin-card">
          <header><h2>Báo cáo chờ xử lý</h2><a class="admin-more" href="${ctx}/admin/reports">Xem tất cả ${stats.pendingReports} →</a></header>
          <ul class="admin-list">
            <c:forEach var="r" items="${overview.latestReports}">
              <li><a href="${ctx}/admin/reports"><c:out value="${r.targetName}"/></a>
                <small>${r.productTarget ? 'Bài đăng' : 'Người dùng'} · ${r.reason.label} · ${r.createdDate.toLocalDate()}</small></li>
            </c:forEach>
          </ul>
        </section>
      </c:if>
      <c:if test="${not empty pendingDisputes}">
        <section class="admin-card">
          <header><h2>Khiếu nại đang mở</h2><a class="admin-more" href="${ctx}/admin/disputes">Xem tất cả ${stats.openDisputes} →</a></header>
          <ul class="admin-list">
            <c:forEach var="d" items="${pendingDisputes}" end="4">
              <li><a href="${ctx}/admin/disputes?id=${d.id}">${d.reason.displayName}</a>
                <small>${d.status.displayName} · mở ngày ${d.createdDate}</small></li>
            </c:forEach>
          </ul>
        </section>
      </c:if>
    </div>
  </c:if>

  <h2 class="admin-section">Số liệu</h2>
  <div class="admin-stats">
    <a class="admin-stat" href="${ctx}/admin/users">
      <strong>${stats.customers}</strong><span>Khách hàng</span>
      <small>+${stats.newCustomers7d} trong 7 ngày · ${stats.bannedUsers} bị khoá</small>
    </a>
    <a class="admin-stat" href="${ctx}/admin/products?filter=all">
      <strong>${stats.products}</strong><span>Bài đăng</span>
      <small>${stats.availableProducts} đang hiện trên bảng tin</small>
    </a>
    <div class="admin-stat">
      <strong>${stats.orders}</strong><span>Đơn hàng</span>
      <small>+${stats.orders7d} trong 7 ngày</small>
    </div>
    <div class="admin-stat">
      <strong><fmt:formatNumber value="${stats.gmv}" pattern="#,##0"/> đ</strong><span>Giá trị đơn đã giao</span>
    </div>
    <div class="admin-stat">
      <strong><fmt:formatNumber value="${stats.commission}" pattern="#,##0"/> đ</strong><span>Doanh thu phí sàn</span>
      <small>Hoa hồng trên các khoản đã giải ngân</small>
    </div>
  </div>

  <%-- Đơn hàng theo trạng thái (theo vòng đời đơn) --%>
  <c:if test="${not empty overview.orderStatuses}">
    <div class="admin-card">
      <h2>Đơn hàng theo trạng thái</h2>
      <div class="admin-status-bar">
        <c:forEach var="s" items="${overview.orderStatuses}">
          <span class="admin-status-chip"><strong>${s.count}</strong> ${s.status.label()}</span>
        </c:forEach>
      </div>
    </div>
  </c:if>

  <%-- Tác vụ tự chạy mỗi giờ (common/ScheduledJobs); nút để chạy ngay khi thử --%>
  <form method="post" action="${ctx}/admin/dashboard" class="admin-card admin-jobs">
    <h2>Tác vụ định kỳ</h2>
    <p class="admin-meta">Tự chạy mỗi giờ: hoàn tất đơn đã giao quá 7 ngày không khiếu nại, chuyển tiền cho người bán
      vừa khai tài khoản nhận tiền, đánh dấu voucher hết hạn.</p>
    <button type="submit">Chạy ngay</button>
  </form>
</div>
<%@ include file="/WEB-INF/views/common/footer.jsp" %>
