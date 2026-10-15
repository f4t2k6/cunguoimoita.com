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
    <a class="admin-stat todo" href="${ctx}/ui?page=dispute">
      <strong>${stats.openDisputes}</strong><span>Khiếu nại đang mở</span>
    </a>
  </div>

  <div class="admin-stats">
    <a class="admin-stat" href="${ctx}/admin/users">
      <strong>${stats.customers}</strong><span>Khách hàng</span>
      <small>${stats.bannedUsers} tài khoản bị khoá</small>
    </a>
    <a class="admin-stat" href="${ctx}/admin/products?filter=all">
      <strong>${stats.products}</strong><span>Bài đăng</span>
      <small>${stats.availableProducts} đang hiện trên bảng tin</small>
    </a>
    <div class="admin-stat">
      <strong>${stats.orders}</strong><span>Đơn hàng</span>
    </div>
    <div class="admin-stat">
      <strong><fmt:formatNumber value="${stats.gmv}" pattern="#,##0"/> đ</strong><span>Giá trị đơn đã giao</span>
    </div>
    <div class="admin-stat">
      <strong><fmt:formatNumber value="${stats.commission}" pattern="#,##0"/> đ</strong><span>Doanh thu phí sàn</span>
      <small>Hoa hồng trên các khoản đã giải ngân</small>
    </div>
  </div>
</div>
<%@ include file="/WEB-INF/views/common/footer.jsp" %>
