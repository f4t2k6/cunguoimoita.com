<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="Người dùng"/>
<c:set var="pageCss" value="admin.css"/>
<c:set var="pageMainClass" value="page-wide"/>
<%@ include file="/WEB-INF/views/common/header.jsp" %>
<div class="admin-page page-card">
  <div class="page-card-head">
    <p class="page-eyebrow">QUẢN LÝ</p>
    <h1 class="page-title">Người dùng</h1>
  </div>
  <%@ include file="/WEB-INF/views/admin/_menu.jsp" %>

  <form class="admin-toolbar" method="get">
    <input type="search" name="q" value="${fn:escapeXml(q)}" placeholder="Tên, email hoặc số điện thoại">
    <select name="status" aria-label="Trạng thái">
      <option value="">Mọi trạng thái</option>
      <option value="active" ${status == 'active' ? 'selected' : ''}>Đang hoạt động</option>
      <option value="banned" ${status == 'banned' ? 'selected' : ''}>Bị khoá</option>
    </select>
    <button type="submit">Tìm</button>
  </form>

  <c:if test="${empty users}"><p class="admin-empty">Không tìm thấy người dùng nào.</p></c:if>
  <c:if test="${fn:length(users) >= 100}"><p class="admin-note">Đang hiện 100 dòng mới nhất. Dùng ô tìm kiếm hoặc bộ lọc để tìm dòng cũ hơn.</p></c:if>
  <div class="admin-table-wrap">
    <table class="admin-table">
      <thead>
        <tr><th>Người dùng</th><th>Liên hệ</th><th>Hạng</th><th>Bài đăng</th><th>Bị báo cáo</th><th>Trạng thái</th><th></th></tr>
      </thead>
      <tbody>
        <c:forEach var="u" items="${users}">
          <tr class="${u.banned ? 'is-off' : ''}">
            <td><strong><c:out value="${u.fullname}"/></strong><br><small>${u.admin ? 'Admin' : 'Khách hàng'} · tham gia ${u.joinDate}</small></td>
            <td><c:out value="${u.email}"/><br><small><c:out value="${u.phone}"/></small></td>
            <td><c:out value="${empty u.tierName ? '—' : u.tierName}"/></td>
            <td>${u.productCount}</td>
            <td>${u.reportCount}</td>
            <td><span class="admin-badge ${u.banned ? 'off' : 'ok'}">${u.banned ? 'Bị khoá' : 'Hoạt động'}</span></td>
            <td>
              <c:if test="${not u.admin}">
                <form method="post">
                  <input type="hidden" name="id" value="${fn:escapeXml(u.id)}">
                  <input type="hidden" name="q" value="${fn:escapeXml(q)}"><input type="hidden" name="status" value="${fn:escapeXml(status)}">
                  <c:choose>
                    <c:when test="${u.banned}"><button name="action" value="unban" class="light">Mở khoá</button></c:when>
                    <c:otherwise>
                      <button name="action" value="ban"
                              onclick="return confirm('Khoá tài khoản này? Người dùng sẽ không đăng nhập được và các bài đang bán bị ẩn.')">Khoá</button>
                    </c:otherwise>
                  </c:choose>
                </form>
              </c:if>
            </td>
          </tr>
        </c:forEach>
      </tbody>
    </table>
  </div>
</div>
<%@ include file="/WEB-INF/views/common/footer.jsp" %>
