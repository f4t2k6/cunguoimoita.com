<%@ page pageEncoding="UTF-8" %>
<%--
  Một bài đăng hiện dạng post trên bảng tin (dùng chung cho trang chủ và trang tìm kiếm).
  Trang gọi đặt biến "post" (Product) rồi <%@ include file="/WEB-INF/views/feed/_post.jsp" %>.
  Cần sẵn: taglib c, fmt; biến ctx (header.jsp đã đặt) và backUrl (trang quay về sau khi bấm Lưu).
--%>
<c:url var="postUrl" value="/product"><c:param name="id" value="${post.id}"/></c:url>
<c:set var="isMine" value="${not empty sessionScope.userId and post.sellerId == sessionScope.userId}"/>
<article class="post-card feed-post">
  <header class="post-head">
    <c:choose>
      <c:when test="${not empty post.sellerAvatar}"><img src="${ctx}<c:out value='${post.sellerAvatar}'/>" alt="" class="post-avatar"></c:when>
      <c:otherwise><img src="${ctx}/assets/images/default-avatar.png" alt="" class="post-avatar"></c:otherwise>
    </c:choose>
    <div class="post-meta">
      <a class="post-author" href="${ctx}/users/profile?id=${fn:escapeXml(post.sellerId)}"><c:out value="${post.sellerName}"/></a>
      <span class="post-date">
        <c:out value="${post.postedDateLabel}"/> ·
        <a class="post-category" href="${ctx}/feed/search?category=${fn:escapeXml(post.categoryId)}"><c:out value="${post.categoryName}"/></a>
      </span>
    </div>
    <span class="listing-pill listing-${post.listingType.code}">${post.listingType.label}</span>
  </header>

  <a class="post-body" href="${postUrl}">
    <p class="post-caption">
      <span class="tag tag-${post.condition.code}">${post.condition.label}</span><strong class="post-title"><c:out value="${post.name}"/></strong>
    </p>
    <c:if test="${not empty post.description}">
      <p class="post-desc"><c:out value="${post.description}"/></p>
    </c:if>
    <c:if test="${post.listingType.forSwap and not empty post.desiredExchangeNote}">
      <p class="post-swap">🔄 Muốn đổi: <c:out value="${post.desiredExchangeNote}"/></p>
    </c:if>
  </a>

  <div class="post-footer">
    <span class="post-price">
      <c:choose>
        <c:when test="${post.listingType.forSale}"><fmt:formatNumber value="${post.price}" pattern="#,##0"/> đ</c:when>
        <c:otherwise><small>Giá trị khoảng</small> <fmt:formatNumber value="${post.estimatedValue}" pattern="#,##0"/> đ</c:otherwise>
      </c:choose>
    </span>
    <c:choose>
      <c:when test="${isMine}">
        <span class="post-mine">Bài của bạn</span>
        <a class="btn" href="${ctx}/product/edit?id=${fn:escapeXml(post.id)}">Sửa bài</a>
      </c:when>
      <c:when test="${empty sessionScope.userId}">
        <%-- Khách: các nút dẫn tới trang đăng nhập, đăng nhập xong quay lại trang này --%>
        <a class="btn" href="${loginUrl}">Lưu</a>
        <a class="btn" href="${loginUrl}">Nhắn tin</a>
        <c:if test="${post.listingType.forSale}"><a class="btn btn-dark" href="${loginUrl}">Thêm vào giỏ</a></c:if>
      </c:when>
      <c:otherwise>
        <form method="post" action="${ctx}/feed/wishlist" class="js-save">
          <input type="hidden" name="productId" value="${fn:escapeXml(post.id)}">
          <input type="hidden" name="action" value="${post.saved ? 'remove' : 'save'}">
          <input type="hidden" name="back" value="${fn:escapeXml(backUrl)}">
          <button class="btn btn-save ${post.saved ? 'is-saved' : ''}" type="submit" aria-pressed="${post.saved}">${post.saved ? 'Đã lưu' : 'Lưu'}</button>
        </form>
        <form method="post" action="${ctx}/chat/start">
          <input type="hidden" name="productId" value="${fn:escapeXml(post.id)}">
          <button class="btn" type="submit">Nhắn tin</button>
        </form>
        <c:if test="${post.listingType.forSale}">
          <form method="post" action="${ctx}/order/cart">
            <input type="hidden" name="action" value="add">
            <input type="hidden" name="productId" value="${fn:escapeXml(post.id)}">
            <button class="btn btn-dark" type="submit">Thêm vào giỏ</button>
          </form>
        </c:if>
      </c:otherwise>
    </c:choose>
  </div>

  <%-- Ảnh: tối đa 3 ô, ô cuối hiện "+N" nếu bài còn nhiều ảnh hơn; bài không có ảnh thì không có khung ảnh --%>
  <c:set var="photoCount" value="${fn:length(post.images)}"/>
  <c:if test="${photoCount > 0}">
    <a class="post-gallery count-${photoCount > 3 ? 3 : photoCount}" href="${postUrl}" aria-label="Xem chi tiết bài">
      <c:forEach var="img" items="${post.images}" end="2" varStatus="ph">
        <span class="post-photo">
          <img src="${ctx}<c:out value='${img}'/>" alt="<c:out value='${post.name}'/>" loading="lazy">
          <c:if test="${ph.last and photoCount > 3}"><span class="post-photo-more">+${photoCount - 3}</span></c:if>
        </span>
      </c:forEach>
    </a>
  </c:if>
</article>
