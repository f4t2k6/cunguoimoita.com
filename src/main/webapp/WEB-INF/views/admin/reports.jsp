<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="Báo cáo vi phạm"/>
<c:set var="pageCss" value="admin.css"/>
<c:set var="pageMainClass" value="page-wide"/>
<%@ include file="/WEB-INF/views/common/header.jsp" %>
<div class="admin-page">
  <%@ include file="/WEB-INF/views/admin/_menu.jsp" %>
  <h1>Báo cáo vi phạm</h1>

  <form class="admin-toolbar" method="get">
    <div class="admin-filter">
      <c:forEach var="f" items="${['pending', 'resolved', 'rejected', 'all']}">
        <label class="${status == f ? 'active' : ''}">
          <input type="radio" name="status" value="${f}" ${status == f ? 'checked' : ''} onchange="this.form.submit()">
          ${f == 'pending' ? 'Chờ xử lý' : f == 'resolved' ? 'Đã xử lý' : f == 'rejected' ? 'Đã bác bỏ' : 'Tất cả'}
        </label>
      </c:forEach>
    </div>
  </form>

  <c:if test="${empty reports}"><p class="admin-empty">Không có báo cáo nào.</p></c:if>
  <div class="admin-cards">
    <c:forEach var="r" items="${reports}">
      <article class="admin-card">
        <header>
          <h2>
            ${r.productTarget ? 'Bài đăng' : 'Người dùng'}:
            <c:out value="${empty r.targetName ? '(không còn tồn tại)' : r.targetName}"/>
          </h2>
          <span class="admin-badge ${r.pending ? 'warn' : r.status == 'resolved' ? 'ok' : 'off'}">
            ${r.pending ? 'Chờ xử lý' : r.status == 'resolved' ? 'Đã xử lý' : 'Đã bác bỏ'}
          </span>
        </header>
        <p class="admin-meta">
          Lý do: <strong>${r.reason.label}</strong> · gửi bởi <c:out value="${r.reporterName}"/>
          · <time>${fn:replace(fn:substring(r.createdDate, 0, 16), 'T', ' ')}</time>
        </p>
        <c:if test="${not empty r.description}"><p class="admin-desc"><c:out value="${r.description}"/></p></c:if>

        <c:if test="${r.pending}">
          <div class="admin-actions">
            <form method="post" class="admin-inline">
              <input type="hidden" name="id" value="${fn:escapeXml(r.id)}">
              <input type="hidden" name="status" value="${status}">
              <input type="hidden" name="action" value="resolve">
              <select name="do" aria-label="Cách xử lý">
                <c:if test="${r.productTarget}"><option value="hide_product">Ẩn bài đăng</option></c:if>
                <option value="ban_user">Khoá ${r.productTarget ? 'người bán' : 'tài khoản này'}</option>
                <option value="none">Chỉ ghi nhận (nhắc nhở)</option>
              </select>
              <button type="submit">Xử lý</button>
            </form>
            <form method="post">
              <input type="hidden" name="id" value="${fn:escapeXml(r.id)}">
              <input type="hidden" name="status" value="${status}">
              <button name="action" value="reject" class="light">Bác bỏ</button>
            </form>
          </div>
        </c:if>
      </article>
    </c:forEach>
  </div>
</div>
<%@ include file="/WEB-INF/views/common/footer.jsp" %>
