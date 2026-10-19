<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="pageTitle" value="Trang chủ"/>
<c:set var="pageCss" value="product.css"/>
<c:set var="hideBack" value="true"/>
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<nav class="category-bar" aria-label="Danh mục">
  <c:forEach var="cat" items="${categories}">
    <a class="category-item" href="${ctx}/feed/search?category=${fn:escapeXml(cat.slug)}">
      <span class="cat-icon">${cat.icon}</span>
      <span><c:out value="${cat.name}"/></span>
    </a>
  </c:forEach>
</nav>

<%-- Ô "Bạn muốn thanh lý gì?" giống ô đăng bài của mạng xã hội --%>
<c:if test="${sessionScope.userRole != 'admin'}">
  <section class="composer">
    <c:choose>
      <c:when test="${not empty sessionScope.avatarPath}"><img src="${ctx}${sessionScope.avatarPath}" alt="" class="post-avatar"></c:when>
      <c:otherwise><img src="${ctx}/assets/images/default-avatar.png" alt="" class="post-avatar"></c:otherwise>
    </c:choose>
    <a class="composer-input" href="${ctx}/product/new">
      <c:out value="${empty sessionScope.fullName ? 'Bạn' : sessionScope.fullName}"/> ơi, bạn muốn thanh lý món gì?
    </a>
    <a class="btn btn-dark composer-btn" href="${ctx}/product/new">Đăng bài</a>
  </section>
</c:if>

<c:forEach var="post" items="${posts}">
  <%@ include file="/WEB-INF/views/feed/_post.jsp" %>
</c:forEach>

<c:if test="${empty posts}">
  <section class="post-card empty-state">
    <div class="empty-icon">🪴</div>
    <h2>Chưa có bài đăng nào</h2>
    <p>Bài đăng mới sẽ hiện ở đây sau khi admin duyệt.</p>
    <a class="btn btn-dark" href="${ctx}/product/new">Đăng món đầu tiên</a>
  </section>
</c:if>

<c:if test="${result.totalPages > 1}">
  <nav class="pager" aria-label="Phân trang">
    <c:if test="${result.hasPrevious}"><a class="btn" href="${ctx}/home?page=${result.page - 1}">← Bài mới hơn</a></c:if>
    <span class="pager-info">Trang ${result.page} / ${result.totalPages}</span>
    <c:if test="${result.hasNext}"><a class="btn btn-dark" href="${ctx}/home?page=${result.page + 1}">Xem thêm bài →</a></c:if>
  </nav>
</c:if>

<script src="${ctx}/assets/js/product.js?v=${assetVer}" defer></script>
<%@ include file="/WEB-INF/views/common/footer.jsp" %>
