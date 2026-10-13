<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="pageTitle" value="Bảng tin"/>
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<nav class="category-bar">
  <c:forEach var="cat" items="${categories}">
    <a class="category-item" href="${ctx}/feed/search?category=${cat.slug}">
      <span class="cat-icon">${cat.icon}</span>
      <span>${cat.name}</span>
    </a>
  </c:forEach>
</nav>

<c:forEach var="post" items="${posts}" varStatus="st">
  <article class="post-card">
    <div class="post-head">
      <img src="${ctx}/assets/images/avt.png" alt="" class="post-avatar">
      <div>
        <span class="post-author"><c:out value="${post.author}"/></span>
        <span class="post-date">${post.date}</span>
      </div>
    </div>
    <p class="post-caption">
      <span class="tag tag-${post.quality}">${post.qualityLabel}</span><c:out value="${post.caption}"/>
    </p>
    <div class="post-footer">
      <span class="post-price"><fmt:formatNumber value="${post.price}" pattern="#,##0"/> đ</span>
      <form method="post" action="${ctx}/feed/wishlist"><input type="hidden" name="productId" value="${post.id}">
        <button class="btn" type="submit">Lưu</button></form>
      <form method="post" action="${ctx}/chat/start">
        <input type="hidden" name="productId" value="${post.id}">
        <button class="btn" type="submit">Nhắn tin</button>
      </form>
      <form method="post" action="${ctx}/order/cart"><input type="hidden" name="productId" value="${post.id}">
        <button class="btn btn-dark" type="submit">Thêm vào giỏ</button></form>
    </div>
    <div class="post-photos">
      <c:forEach var="photo" items="${post.photos}">
        <div class="post-photo">${photo}</div>
      </c:forEach>
    </div>
  </article>

  <%-- Dải reels chen sau bài đầu tiên --%>
  <c:if test="${st.first}">
    <section class="post-card">
      <div class="reel-strip">
        <c:forEach var="reel" items="${reels}">
          <div class="reel-card"><span>${reel}</span></div>
        </c:forEach>
      </div>
    </section>
  </c:if>
</c:forEach>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
