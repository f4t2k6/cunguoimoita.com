<%@ page pageEncoding="UTF-8" %>
<%-- Thanh chuyển trang của khu quản trị + thông báo kết quả thao tác (flash). Include tĩnh ở đầu mỗi trang admin. --%>
<nav class="admin-tabs" aria-label="Quản trị">
  <a href="${ctx}/admin/dashboard" class="${adminTab == 'dashboard' ? 'active' : ''}">Tổng quan</a>
  <a href="${ctx}/admin/products" class="${adminTab == 'products' ? 'active' : ''}">Duyệt bài</a>
  <a href="${ctx}/admin/reports" class="${adminTab == 'reports' ? 'active' : ''}">Báo cáo vi phạm</a>
  <a href="${ctx}/admin/users" class="${adminTab == 'users' ? 'active' : ''}">Người dùng</a>
  <a href="${ctx}/admin/categories" class="${adminTab == 'categories' ? 'active' : ''}">Danh mục</a>
  <a href="${ctx}/admin/carriers" class="${adminTab == 'carriers' ? 'active' : ''}">Vận chuyển</a>
  <a href="${ctx}/admin/tiers" class="${adminTab == 'tiers' ? 'active' : ''}">Hạng thành viên</a>
  <a href="${ctx}/admin/disputes" class="${adminTab == 'disputes' ? 'active' : ''}">Khiếu nại</a>
</nav>
<c:if test="${not empty flash}"><p class="admin-flash" role="status"><c:out value="${flash}"/></p></c:if>
<c:if test="${not empty flashError}"><p class="admin-flash error" role="alert"><c:out value="${flashError}"/></p></c:if>
