<%@ page pageEncoding="UTF-8" %>
<%-- Thanh chuyển trang của khu quản trị + thông báo kết quả thao tác (flash). Include tĩnh ở đầu mỗi trang admin.
     Chia 2 nhóm: việc cần xử lý (có số đang chờ, lấy từ adminTodo của AdminBaseServlet) và danh mục quản lý.
     Màn hình nhỏ: thanh menu thành 1 hàng kéo ngang thay vì xuống 3 dòng. --%>
<nav class="admin-tabs" aria-label="Quản trị">
  <a href="${ctx}/admin/dashboard" class="${adminTab == 'dashboard' ? 'active' : ''}">Tổng quan</a>
  <span class="admin-tabs-sep" aria-hidden="true"></span>
  <a href="${ctx}/admin/products" class="${adminTab == 'products' ? 'active' : ''}">Duyệt bài<c:if test="${adminTodo.pendingProducts > 0}"> <b class="admin-count" title="Bài chờ duyệt">${adminTodo.pendingProducts}</b></c:if></a>
  <a href="${ctx}/admin/reports" class="${adminTab == 'reports' ? 'active' : ''}">Báo cáo<c:if test="${adminTodo.pendingReports > 0}"> <b class="admin-count" title="Báo cáo chờ xử lý">${adminTodo.pendingReports}</b></c:if></a>
  <a href="${ctx}/admin/disputes" class="${adminTab == 'disputes' ? 'active' : ''}">Khiếu nại<c:if test="${adminTodo.openDisputes > 0}"> <b class="admin-count" title="Khiếu nại đang mở">${adminTodo.openDisputes}</b></c:if></a>
  <span class="admin-tabs-sep" aria-hidden="true"></span>
  <a href="${ctx}/admin/users" class="${adminTab == 'users' ? 'active' : ''}">Người dùng</a>
  <a href="${ctx}/admin/vouchers" class="${adminTab == 'vouchers' ? 'active' : ''}">Voucher</a>
  <a href="${ctx}/admin/categories" class="${adminTab == 'categories' ? 'active' : ''}">Danh mục</a>
  <a href="${ctx}/admin/carriers" class="${adminTab == 'carriers' ? 'active' : ''}">Vận chuyển</a>
  <a href="${ctx}/admin/tiers" class="${adminTab == 'tiers' ? 'active' : ''}">Hạng thành viên</a>
</nav>
<c:if test="${not empty flash}"><p class="admin-flash" role="status"><c:out value="${flash}"/></p></c:if>
<c:if test="${not empty flashError}"><p class="admin-flash error" role="alert"><c:out value="${flashError}"/></p></c:if>
