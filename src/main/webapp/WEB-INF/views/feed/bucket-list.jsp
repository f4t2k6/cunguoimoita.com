<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="pageTitle" value="Đã lưu"/>
<c:set var="pageCss" value="product.css"/>
<%@ include file="/WEB-INF/views/common/header.jsp" %>
<div class="page-card">

<section class="search-head">
  <p class="page-eyebrow">BUCKET LIST</p>
  <h1 class="page-title">Đã lưu</h1>
  <p class="page-subtitle">${fn:length(products)} món bạn đang để ý. Món đã bán hoặc bị gỡ vẫn nằm đây cho tới khi bạn bỏ lưu.</p>
</section>

<c:if test="${param.removed == '1'}"><div class="alert alert-success">Đã bỏ lưu món đó khỏi Bucket List.</div></c:if>
<c:if test="${not empty error}"><div class="alert alert-error"><c:out value="${error}"/></div></c:if>

<c:choose>
  <c:when test="${empty products}">
    <section class="post-card empty-state">
      <div class="empty-icon">🔖</div>
      <h2>Bucket List đang trống</h2>
      <p>Bấm nút <strong>Lưu</strong> trên bài đăng để gom các món bạn thích vào đây.</p>
      <a class="btn btn-dark" href="${ctx}/home">Lướt bảng tin</a>
    </section>
  </c:when>
  <c:otherwise>
    <div class="saved-list">
      <c:forEach var="m" items="${products}">
        <article class="saved-item ${m.listed ? '' : 'is-off'}">
          <%@ include file="/WEB-INF/views/product/_mini-card.jsp" %>
          <div class="saved-actions">
            <c:if test="${m.available and m.listingType.forSale}">
              <form method="post" action="${ctx}/order/cart">
                <input type="hidden" name="action" value="add">
                <input type="hidden" name="productId" value="${fn:escapeXml(m.id)}">
                <button class="btn btn-dark" type="submit">Thêm vào giỏ</button>
              </form>
            </c:if>
            <c:if test="${m.listed}">
              <form method="post" action="${ctx}/chat/start">
                <input type="hidden" name="productId" value="${fn:escapeXml(m.id)}">
                <button class="btn" type="submit">Nhắn tin</button>
              </form>
            </c:if>
            <form method="post" action="${ctx}/feed/wishlist">
              <input type="hidden" name="productId" value="${fn:escapeXml(m.id)}">
              <input type="hidden" name="action" value="remove">
              <input type="hidden" name="back" value="/feed/bucket-list?removed=1">
              <button class="btn btn-ghost" type="submit">Bỏ lưu</button>
            </form>
          </div>
        </article>
      </c:forEach>
    </div>
  </c:otherwise>
</c:choose>
</div>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
