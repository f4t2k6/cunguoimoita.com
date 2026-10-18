<%@ page pageEncoding="UTF-8" %>
<%-- Thẻ nhỏ một bài (gợi ý trao đổi, bài khác của người bán, Bucket List). Trang gọi đặt biến "m" (Product). --%>
<a class="mini-card ${m.listed ? '' : 'is-off'}" href="${ctx}/product?id=${fn:escapeXml(m.id)}">
  <span class="mini-photo">
    <c:choose>
      <c:when test="${not empty m.coverImage}"><img src="${ctx}<c:out value='${m.coverImage}'/>" alt="" loading="lazy"></c:when>
      <c:otherwise><span class="post-photo-empty">Chưa có ảnh</span></c:otherwise>
    </c:choose>
    <c:if test="${m.stateCode != 'available'}"><span class="photo-ribbon">${m.stateLabel}</span></c:if>
  </span>
  <span class="mini-body">
    <span class="mini-name"><c:out value="${m.name}"/></span>
    <span class="mini-price">
      <c:choose>
        <c:when test="${m.listingType.forSale}"><fmt:formatNumber value="${m.price}" pattern="#,##0"/> đ</c:when>
        <c:otherwise>~ <fmt:formatNumber value="${m.estimatedValue}" pattern="#,##0"/> đ</c:otherwise>
      </c:choose>
    </span>
    <span class="mini-meta"><span class="tag tag-${m.condition.code}">${m.condition.label}</span> <c:out value="${m.sellerName}"/></span>
  </span>
</a>
