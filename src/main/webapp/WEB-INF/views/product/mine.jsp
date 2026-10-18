<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="pageTitle" value="Bài đăng của tôi"/>
<c:set var="pageCss" value="product.css"/>
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<section class="search-head with-action">
  <div>
    <p class="page-eyebrow">NGƯỜI BÁN</p>
    <h1 class="page-title">Bài đăng của tôi</h1>
    <p class="page-subtitle">Theo dõi bài chờ duyệt, sửa, tạm ẩn hoặc xoá bài của bạn.</p>
  </div>
  <a class="btn btn-dark btn-lg" href="${ctx}/product/new">＋ Đăng bài mới</a>
</section>

<c:if test="${not empty message}"><div class="alert alert-success"><c:out value="${message}"/></div></c:if>
<c:if test="${not empty error}"><div class="alert alert-error"><c:out value="${error}"/></div></c:if>

<%-- Tab lọc theo trạng thái, kèm số bài của từng tab; tab không có bài nào thì ẩn cho gọn --%>
<nav class="tab-row" aria-label="Lọc theo trạng thái">
  <c:forEach var="t" items="${tabCounts}">
    <c:set var="tabLabel" value="${t.key == 'all' ? 'Tất cả' : t.key == 'pending' ? 'Chờ duyệt' : t.key == 'selling' ? 'Đang hiện' : t.key == 'paused' ? 'Đang ẩn' : t.key == 'done' ? 'Đã bán / đổi' : 'Bị từ chối'}"/>
    <c:if test="${t.key == 'all' or t.value > 0 or tab == t.key}">
      <a class="tab ${tab == t.key ? 'active' : ''}" href="${ctx}/product/mine?tab=${t.key}">${tabLabel}<span class="tab-count">${t.value}</span></a>
    </c:if>
  </c:forEach>
</nav>

<c:choose>
  <c:when test="${empty products}">
    <section class="post-card empty-state">
      <div class="empty-icon">📦</div>
      <h2>${tab == 'all' ? 'Bạn chưa đăng bài nào' : 'Không có bài nào ở mục này'}</h2>
      <p>Đồ không dùng nữa? Chụp vài tấm ảnh và đăng lên để người cần tìm thấy.</p>
      <a class="btn btn-dark" href="${ctx}/product/new">Đăng bài</a>
    </section>
  </c:when>
  <c:otherwise>
    <ul class="my-list">
      <c:forEach var="p" items="${products}">
        <li class="my-item">
          <a class="my-thumb" href="${ctx}/product?id=${fn:escapeXml(p.id)}">
            <c:choose>
              <c:when test="${not empty p.coverImage}"><img src="${ctx}<c:out value='${p.coverImage}'/>" alt="" loading="lazy"></c:when>
              <c:otherwise><span class="post-photo-empty">Chưa có ảnh</span></c:otherwise>
            </c:choose>
          </a>
          <div class="my-info">
            <a class="my-name" href="${ctx}/product?id=${fn:escapeXml(p.id)}"><c:out value="${p.name}"/></a>
            <span class="my-price">
              <c:choose>
                <c:when test="${p.listingType.forSale}"><fmt:formatNumber value="${p.price}" pattern="#,##0"/> đ</c:when>
                <c:otherwise>~ <fmt:formatNumber value="${p.estimatedValue}" pattern="#,##0"/> đ</c:otherwise>
              </c:choose>
              · ${p.listingType.label} · <c:out value="${p.categoryName}"/>
            </span>
            <span class="my-meta">
              <span class="state-pill state-${p.stateCode}">${p.stateLabel}</span>
              Đăng ngày <c:out value="${p.postedDateLabel}"/>
            </span>
            <c:if test="${p.stateCode == 'rejected'}"><span class="my-hint">Admin đã gửi lý do trong thông báo. Sửa bài để gửi duyệt lại.</span></c:if>
            <c:if test="${p.stateCode == 'hidden'}"><span class="my-hint">Bài bị admin ẩn do vi phạm nên bạn không tự hiện lại được.</span></c:if>
          </div>
          <div class="my-actions">
            <c:if test="${p.editable}"><a class="btn" href="${ctx}/product/edit?id=${fn:escapeXml(p.id)}">Sửa</a></c:if>
            <c:if test="${p.pausable or p.resumable}">
              <form method="post" action="${ctx}/product/mine">
                <input type="hidden" name="id" value="${fn:escapeXml(p.id)}">
                <input type="hidden" name="action" value="${p.pausable ? 'pause' : 'resume'}">
                <input type="hidden" name="back" value="/product/mine?tab=${tab}">
                <button class="btn" type="submit">${p.pausable ? 'Ẩn' : 'Hiện lại'}</button>
              </form>
            </c:if>
            <c:if test="${p.editable}">
              <form method="post" action="${ctx}/product/mine" class="js-confirm" data-confirm="Xoá hẳn bài “${fn:escapeXml(p.name)}”? Không khôi phục được.">
                <input type="hidden" name="id" value="${fn:escapeXml(p.id)}">
                <input type="hidden" name="action" value="delete">
                <button class="btn btn-danger" type="submit">Xoá</button>
              </form>
            </c:if>
          </div>
        </li>
      </c:forEach>
    </ul>
  </c:otherwise>
</c:choose>

<script src="${ctx}/assets/js/product.js?v=${assetVer}" defer></script>
<%@ include file="/WEB-INF/views/common/footer.jsp" %>
