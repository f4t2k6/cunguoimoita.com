<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="pageTitle" value="${not empty param.q ? 'Tìm “'.concat(param.q).concat('”') : (not empty activeCategory ? activeCategory.name : 'Tìm kiếm')}"/>
<c:set var="pageCss" value="product.css"/>
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<section class="search-head">
  <p class="page-eyebrow">TÌM KIẾM</p>
  <h1 class="page-title">
    <c:choose>
      <c:when test="${not empty param.q}">“<c:out value="${param.q}"/>”</c:when>
      <c:when test="${not empty activeCategory}"><c:out value="${activeCategory.name}"/></c:when>
      <c:otherwise>Tất cả bài đăng</c:otherwise>
    </c:choose>
  </h1>
  <p class="page-subtitle">${result.total} bài phù hợp<c:if test="${not empty activeCategory and not empty param.q}"> trong <c:out value="${activeCategory.name}"/></c:if></p>
</section>

<%-- Bộ lọc: gửi GET nên đường dẫn kết quả có thể chia sẻ / bấm Back được --%>
<form class="filter-card" method="get" action="${ctx}/feed/search">
  <div class="filter-row">
    <label class="filter-field filter-wide">
      <span>Từ khoá</span>
      <input type="search" name="q" value="${fn:escapeXml(param.q)}" placeholder="Tên món, mô tả..." maxlength="100">
    </label>
    <label class="filter-field">
      <span>Danh mục</span>
      <select name="category">
        <option value="">Tất cả danh mục</option>
        <c:forEach var="cg" items="${categories}">
          <option value="${fn:escapeXml(cg.id)}" ${cg.id == param.category ? 'selected' : ''}>${cg.topLevel ? '' : '— '}<c:out value="${cg.name}"/></option>
        </c:forEach>
      </select>
    </label>
  </div>
  <div class="filter-row">
    <label class="filter-field">
      <span>Giá từ (đ)</span>
      <input type="text" inputmode="numeric" name="minPrice" value="${fn:escapeXml(param.minPrice)}" placeholder="0" class="js-money">
    </label>
    <label class="filter-field">
      <span>Đến (đ)</span>
      <input type="text" inputmode="numeric" name="maxPrice" value="${fn:escapeXml(param.maxPrice)}" placeholder="Bất kỳ" class="js-money">
    </label>
    <label class="filter-field">
      <span>Tình trạng</span>
      <select name="condition">
        <option value="">Tất cả</option>
        <option value="new" ${param.condition == 'new' ? 'selected' : ''}>Mới</option>
        <option value="used" ${param.condition == 'used' ? 'selected' : ''}>Đã qua sử dụng</option>
        <option value="damaged" ${param.condition == 'damaged' ? 'selected' : ''}>Hỏng nhẹ</option>
      </select>
    </label>
    <label class="filter-field">
      <span>Loại bài</span>
      <select name="type">
        <option value="">Tất cả</option>
        <option value="sale" ${param.type == 'sale' ? 'selected' : ''}>Có bán</option>
        <option value="swap" ${param.type == 'swap' ? 'selected' : ''}>Có nhận đổi</option>
      </select>
    </label>
    <label class="filter-field">
      <span>Sắp xếp</span>
      <select name="sort">
        <option value="newest">Mới nhất</option>
        <option value="price_asc" ${param.sort == 'price_asc' ? 'selected' : ''}>Giá thấp → cao</option>
        <option value="price_desc" ${param.sort == 'price_desc' ? 'selected' : ''}>Giá cao → thấp</option>
      </select>
    </label>
  </div>
  <div class="filter-actions">
    <a class="btn" href="${ctx}/feed/search">Xoá bộ lọc</a>
    <button class="btn btn-dark" type="submit">Lọc kết quả</button>
  </div>
</form>

<c:if test="${not empty filterError}"><div class="alert alert-error"><c:out value="${filterError}"/></div></c:if>

<c:forEach var="post" items="${result.items}">
  <%@ include file="/WEB-INF/views/feed/_post.jsp" %>
</c:forEach>

<c:if test="${empty result.items}">
  <section class="post-card empty-state">
    <div class="empty-icon">🔍</div>
    <h2>Không tìm thấy bài nào</h2>
    <p>Thử từ khoá ngắn hơn, bỏ bớt bộ lọc hoặc chọn danh mục khác.</p>
    <a class="btn" href="${ctx}/feed/search">Xem tất cả bài</a>
  </section>
</c:if>

<c:if test="${result.totalPages > 1}">
  <nav class="pager" aria-label="Phân trang">
    <c:if test="${result.hasPrevious}">
      <c:url var="prevUrl" value="/feed/search">
        <c:param name="q" value="${param.q}"/><c:param name="category" value="${param.category}"/>
        <c:param name="minPrice" value="${param.minPrice}"/><c:param name="maxPrice" value="${param.maxPrice}"/>
        <c:param name="condition" value="${param.condition}"/><c:param name="type" value="${param.type}"/>
        <c:param name="sort" value="${param.sort}"/><c:param name="page" value="${result.page - 1}"/>
      </c:url>
      <a class="btn" href="${prevUrl}">← Trang trước</a>
    </c:if>
    <span class="pager-info">Trang ${result.page} / ${result.totalPages}</span>
    <c:if test="${result.hasNext}">
      <c:url var="nextUrl" value="/feed/search">
        <c:param name="q" value="${param.q}"/><c:param name="category" value="${param.category}"/>
        <c:param name="minPrice" value="${param.minPrice}"/><c:param name="maxPrice" value="${param.maxPrice}"/>
        <c:param name="condition" value="${param.condition}"/><c:param name="type" value="${param.type}"/>
        <c:param name="sort" value="${param.sort}"/><c:param name="page" value="${result.page + 1}"/>
      </c:url>
      <a class="btn btn-dark" href="${nextUrl}">Trang sau →</a>
    </c:if>
  </nav>
</c:if>

<script src="${ctx}/assets/js/product.js?v=${assetVer}" defer></script>
<%@ include file="/WEB-INF/views/common/footer.jsp" %>
