<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="pageTitle" value="${product.name}"/>
<c:set var="pageCss" value="product.css"/>
<c:set var="pageMainClass" value="page-wide"/>
<%@ include file="/WEB-INF/views/common/header.jsp" %>
<c:set var="backUrl" value="/product?id=${product.id}"/>

<nav class="crumbs" aria-label="Đường dẫn">
  <a href="${ctx}/home">Trang chủ</a>
  <c:if test="${not empty product.parentCategoryId}">
    <span>›</span><a href="${ctx}/feed/search?category=${fn:escapeXml(product.parentCategoryId)}"><c:out value="${product.parentCategoryName}"/></a>
  </c:if>
  <span>›</span><a href="${ctx}/feed/search?category=${fn:escapeXml(product.categoryId)}"><c:out value="${product.categoryName}"/></a>
</nav>

<c:if test="${not empty message}"><div class="alert alert-success"><c:out value="${message}"/></div></c:if>
<c:if test="${not empty error}"><div class="alert alert-error"><c:out value="${error}"/></div></c:if>

<%-- Người bán / admin xem bài chưa hiện công khai thì có dòng giải thích --%>
<c:choose>
  <c:when test="${product.stateCode == 'pending'}"><div class="alert alert-info">Bài đang chờ admin duyệt. Chỉ bạn và admin thấy trang này.</div></c:when>
  <c:when test="${product.stateCode == 'rejected'}"><div class="alert alert-error">Bài bị admin từ chối. Sửa lại nội dung để gửi duyệt lại.</div></c:when>
  <c:when test="${product.stateCode == 'paused'}"><div class="alert alert-info">Bạn đang tạm ẩn bài này, khách không thấy bài.</div></c:when>
  <c:when test="${product.stateCode == 'hidden'}"><div class="alert alert-error">Bài đã bị admin ẩn do vi phạm.</div></c:when>
  <c:when test="${product.stateCode == 'sold' or product.stateCode == 'exchanged'}"><div class="alert alert-info">Món này đã giao dịch xong.</div></c:when>
</c:choose>

<div class="detail-layout">
  <%-- Cột trái: ảnh lớn + dải ảnh nhỏ (bấm ảnh nhỏ để đổi ảnh lớn) --%>
  <section class="detail-gallery">
    <div class="detail-main-photo">
      <c:choose>
        <c:when test="${empty product.images}"><span class="post-photo-empty">Chưa có ảnh</span></c:when>
        <c:otherwise><img id="mainPhoto" src="${ctx}<c:out value='${product.coverImage}'/>" alt="<c:out value='${product.name}'/>"></c:otherwise>
      </c:choose>
      <c:if test="${product.stateCode == 'reserved'}"><span class="photo-ribbon">Đã có người đặt</span></c:if>
    </div>
    <c:if test="${fn:length(product.images) > 1}">
      <div class="detail-thumbs" role="list">
        <c:forEach var="img" items="${product.images}" varStatus="st">
          <button type="button" class="thumb ${st.first ? 'active' : ''}" data-src="${ctx}<c:out value='${img}'/>" aria-label="Ảnh ${st.count}">
            <img src="${ctx}<c:out value='${img}'/>" alt="" loading="lazy">
          </button>
        </c:forEach>
      </div>
    </c:if>
  </section>

  <%-- Cột phải: thông tin và các nút --%>
  <section class="detail-info">
    <div class="detail-tags">
      <span class="tag tag-${product.condition.code}">${product.condition.label}</span>
      <span class="listing-pill listing-${product.listingType.code}">${product.listingType.label}</span>
      <span class="state-pill state-${product.stateCode}">${product.stateLabel}</span>
    </div>
    <h1 class="detail-title"><c:out value="${product.name}"/></h1>

    <div class="detail-prices">
      <c:if test="${product.listingType.forSale}">
        <div>
          <span class="price-label">Giá bán</span>
          <span class="price-big"><fmt:formatNumber value="${product.price}" pattern="#,##0"/> đ</span>
        </div>
      </c:if>
      <c:if test="${product.listingType.forSwap}">
        <div>
          <span class="price-label">Giá trị ước tính khi đổi</span>
          <span class="price-mid">~ <fmt:formatNumber value="${product.estimatedValue}" pattern="#,##0"/> đ</span>
        </div>
      </c:if>
    </div>

    <c:if test="${product.listingType.forSwap}">
      <div class="swap-box">
        <strong>🔄 Món muốn đổi</strong>
        <p><c:out value="${empty product.desiredExchangeNote ? 'Người bán chưa ghi cụ thể, nhắn tin để hỏi thêm.' : product.desiredExchangeNote}"/></p>
      </div>
    </c:if>

    <ul class="detail-facts">
      <li><span>Danh mục</span><strong><c:out value="${product.categoryName}"/></strong></li>
      <li><span>Tình trạng</span><strong>${product.condition.label}</strong></li>
      <c:if test="${product.listingType.forSale}">
        <li><span>Thanh toán khi nhận (COD)</span><strong>${product.acceptCod ? 'Có nhận' : 'Không nhận'}</strong></li>
      </c:if>
      <li><span>Ngày đăng</span><strong><c:out value="${product.postedDateLabel}"/></strong></li>
    </ul>

    <div class="detail-actions">
      <c:choose>
        <c:when test="${isOwner}">
          <c:if test="${product.editable}"><a class="btn btn-dark btn-lg" href="${ctx}/product/edit?id=${fn:escapeXml(product.id)}">Sửa bài</a></c:if>
          <c:if test="${product.pausable or product.resumable}">
            <form method="post" action="${ctx}/product/mine">
              <input type="hidden" name="id" value="${fn:escapeXml(product.id)}">
              <input type="hidden" name="action" value="${product.pausable ? 'pause' : 'resume'}">
              <input type="hidden" name="back" value="${fn:escapeXml(backUrl)}">
              <button class="btn btn-lg" type="submit">${product.pausable ? 'Tạm ẩn bài' : 'Hiện lại bài'}</button>
            </form>
          </c:if>
          <a class="btn btn-lg" href="${ctx}/product/mine">Bài đăng của tôi</a>
        </c:when>
        <c:when test="${empty sessionScope.userId}">
          <%-- Khách: xem được chi tiết, muốn mua / nhắn tin / lưu thì đăng nhập (xong quay lại trang này) --%>
          <c:if test="${product.listingType.forSale}">
            <a class="btn btn-dark btn-lg" href="${loginUrl}">Thêm vào giỏ</a>
          </c:if>
          <a class="btn btn-lg" href="${loginUrl}">${product.listingType.forSwap ? 'Nhắn tin / đề nghị đổi' : 'Nhắn tin, trả giá'}</a>
          <a class="btn btn-lg" href="${loginUrl}">Lưu</a>
          <p class="hint guest-hint">Bạn đang xem với tư cách khách. <a href="${loginUrl}">Đăng nhập</a> hoặc <a href="${registerUrl}">đăng ký</a> để mua, nhắn tin và lưu bài.</p>
        </c:when>
        <c:when test="${sessionScope.userRole == 'admin'}">
          <a class="btn btn-dark btn-lg" href="${ctx}/admin/products?filter=all&amp;q=${fn:escapeXml(product.name)}">Mở ở trang quản trị</a>
        </c:when>
        <c:otherwise>
          <c:if test="${product.listingType.forSale}">
            <form method="post" action="${ctx}/order/cart">
              <input type="hidden" name="action" value="add">
              <input type="hidden" name="productId" value="${fn:escapeXml(product.id)}">
              <button class="btn btn-dark btn-lg" type="submit" ${product.available ? '' : 'disabled'}>Thêm vào giỏ</button>
            </form>
          </c:if>
          <form method="post" action="${ctx}/chat/start">
            <input type="hidden" name="productId" value="${fn:escapeXml(product.id)}">
            <button class="btn btn-lg" type="submit">${product.listingType.forSwap ? 'Nhắn tin / đề nghị đổi' : 'Nhắn tin, trả giá'}</button>
          </form>
          <form method="post" action="${ctx}/feed/wishlist" class="js-save">
            <input type="hidden" name="productId" value="${fn:escapeXml(product.id)}">
            <input type="hidden" name="action" value="${product.saved ? 'remove' : 'save'}">
            <input type="hidden" name="back" value="${fn:escapeXml(backUrl)}">
            <button class="btn btn-lg btn-save ${product.saved ? 'is-saved' : ''}" type="submit" aria-pressed="${product.saved}">${product.saved ? 'Đã lưu' : 'Lưu'}</button>
          </form>
        </c:otherwise>
      </c:choose>
    </div>

    <%-- Người bán --%>
    <a class="seller-card" href="${ctx}/users/profile?id=${fn:escapeXml(product.sellerId)}">
      <c:choose>
        <c:when test="${not empty product.sellerAvatar}"><img src="${ctx}<c:out value='${product.sellerAvatar}'/>" alt="" class="post-avatar"></c:when>
        <c:otherwise><img src="${ctx}/assets/images/default-avatar.png" alt="" class="post-avatar"></c:otherwise>
      </c:choose>
      <span>
        <span class="seller-label">Người đăng</span>
        <span class="post-author"><c:out value="${product.sellerName}"/></span>
      </span>
      <span class="seller-link">Xem hồ sơ →</span>
    </a>

    <c:if test="${not isOwner and sessionScope.userRole != 'admin'}">
      <a class="report-link" href="${ctx}/report?type=product&amp;id=${fn:escapeXml(product.id)}">Báo cáo bài đăng vi phạm</a>
    </c:if>
  </section>
</div>

<section class="detail-section">
  <h2 class="section-heading">Mô tả</h2>
  <div class="detail-desc"><c:out value="${empty product.description ? 'Người bán chưa viết mô tả.' : product.description}"/></div>
</section>

<%-- Product.suggestMatches(): chỉ có với bài nhận trao đổi --%>
<c:if test="${product.listingType.forSwap}">
  <section class="detail-section">
    <h2 class="section-heading">Gợi ý trao đổi</h2>
    <p class="section-note">Các bài khác cũng nhận đổi, giá trị ước tính gần với món này (lệch tối đa 30%).</p>
    <c:choose>
      <c:when test="${empty matches}"><p class="muted">Chưa có món nào phù hợp, quay lại sau nhé.</p></c:when>
      <c:otherwise>
        <div class="mini-grid">
          <c:forEach var="m" items="${matches}"><%@ include file="/WEB-INF/views/product/_mini-card.jsp" %></c:forEach>
        </div>
      </c:otherwise>
    </c:choose>
  </section>
</c:if>

<c:if test="${not empty sellerOthers}">
  <section class="detail-section">
    <h2 class="section-heading">Bài khác của <c:out value="${product.sellerName}"/></h2>
    <div class="mini-grid">
      <c:forEach var="m" items="${sellerOthers}"><%@ include file="/WEB-INF/views/product/_mini-card.jsp" %></c:forEach>
    </div>
  </section>
</c:if>

<script src="${ctx}/assets/js/product.js?v=${assetVer}" defer></script>
<%@ include file="/WEB-INF/views/common/footer.jsp" %>
