<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN" scope="request"/>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="Duyệt bài"/>
<c:set var="pageCss" value="admin.css"/>
<c:set var="pageMainClass" value="page-wide"/>
<%@ include file="/WEB-INF/views/common/header.jsp" %>
<div class="admin-page page-card">
  <div class="page-card-head">
    <p class="page-eyebrow">KIỂM DUYỆT</p>
    <h1 class="page-title">Bài đăng</h1>
  </div>
  <%@ include file="/WEB-INF/views/admin/_menu.jsp" %>

  <form class="admin-toolbar" method="get">
    <div class="admin-filter">
      <c:forEach var="f" items="${['pending', 'approved', 'hidden', 'rejected', 'all']}">
        <label class="${filter == f ? 'active' : ''}">
          <input type="radio" name="filter" value="${f}" ${filter == f ? 'checked' : ''} onchange="this.form.submit()">
          ${f == 'pending' ? 'Chờ duyệt' : f == 'approved' ? 'Đã duyệt' : f == 'hidden' ? 'Đang ẩn' : f == 'rejected' ? 'Bị từ chối' : 'Tất cả'}
        </label>
      </c:forEach>
    </div>
    <input type="search" name="q" value="${fn:escapeXml(q)}" placeholder="Tên bài hoặc người bán">
    <button type="submit">Tìm</button>
  </form>

  <c:if test="${empty products}"><p class="admin-empty">Không có bài nào.</p></c:if>
  <c:if test="${fn:length(products) >= 100}"><p class="admin-note">Đang hiện 100 dòng mới nhất. Dùng ô tìm kiếm hoặc bộ lọc để tìm dòng cũ hơn.</p></c:if>
  <div class="admin-cards">
    <c:forEach var="p" items="${products}">
      <article class="admin-card">
        <header>
          <h2><c:out value="${p.name}"/></h2>
          <span class="admin-badge ${p.pending ? 'warn' : p.rejected || p.hidden ? 'off' : 'ok'}">
            ${p.pending ? 'Chờ duyệt' : p.rejected ? 'Bị từ chối' : p.hidden ? 'Đang ẩn' : 'Đã duyệt'}
          </span>
        </header>
        <p class="admin-meta">
          <c:out value="${p.sellerName}"/> · <c:out value="${p.categoryName}"/> · đăng ${p.postedDate}
          <c:if test="${not empty p.price}"> · <fmt:formatNumber value="${p.price}" pattern="#,##0"/> đ</c:if>
          · ${p.listingType == 'sale' ? 'Bán' : p.listingType == 'swap' ? 'Trao đổi' : 'Bán hoặc đổi'}
          · ${p.condition == 'new' ? 'Mới' : p.condition == 'used' ? 'Đã dùng' : 'Hỏng nhẹ'}
        </p>
        <c:if test="${not empty p.description}"><p class="admin-desc"><c:out value="${p.description}"/></p></c:if>

        <div class="admin-actions">
          <c:choose>
            <c:when test="${p.pending}">
              <form method="post">
                <input type="hidden" name="id" value="${fn:escapeXml(p.id)}">
                <input type="hidden" name="filter" value="${filter}"><input type="hidden" name="q" value="${fn:escapeXml(q)}">
                <button name="action" value="approve">Duyệt</button>
              </form>
              <form method="post" class="admin-inline">
                <input type="hidden" name="id" value="${fn:escapeXml(p.id)}">
                <input type="hidden" name="filter" value="${filter}"><input type="hidden" name="q" value="${fn:escapeXml(q)}">
                <input name="reason" required maxlength="300" placeholder="Lý do từ chối">
                <button name="action" value="reject" class="light">Từ chối</button>
              </form>
            </c:when>
            <c:when test="${p.hidden and not p.rejected}">
              <form method="post">
                <input type="hidden" name="id" value="${fn:escapeXml(p.id)}">
                <input type="hidden" name="filter" value="${filter}"><input type="hidden" name="q" value="${fn:escapeXml(q)}">
                <button name="action" value="show" class="light">Hiện lại</button>
              </form>
            </c:when>
            <c:when test="${not p.rejected}">
              <form method="post" class="admin-inline">
                <input type="hidden" name="id" value="${fn:escapeXml(p.id)}">
                <input type="hidden" name="filter" value="${filter}"><input type="hidden" name="q" value="${fn:escapeXml(q)}">
                <input name="reason" maxlength="300" placeholder="Lý do ẩn (tuỳ chọn)">
                <button name="action" value="hide" class="light">Ẩn bài</button>
              </form>
            </c:when>
          </c:choose>
        </div>
      </article>
    </c:forEach>
  </div>
</div>
<%@ include file="/WEB-INF/views/common/footer.jsp" %>
