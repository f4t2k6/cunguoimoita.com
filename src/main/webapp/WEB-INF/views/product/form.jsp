<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="pageTitle" value="${editing ? 'Sửa bài đăng' : 'Đăng bài'}"/>
<c:set var="pageCss" value="product.css"/>
<c:set var="backUrl" value="/product/mine"/>
<%@ include file="/WEB-INF/views/common/header.jsp" %>
<div class="page-card">

<%-- Danh mục đang chọn: nếu là danh mục con thì ô cha chọn cha của nó, ô con chọn nó --%>
<c:set var="selectedParent" value="${product.categoryId}"/>
<c:set var="selectedChild" value=""/>
<c:forEach var="cg" items="${categories}">
  <c:if test="${cg.id == product.categoryId and not cg.topLevel}">
    <c:set var="selectedParent" value="${cg.parentCategoryId}"/>
    <c:set var="selectedChild" value="${cg.id}"/>
  </c:if>
</c:forEach>

<section class="search-head">
  <p class="page-eyebrow">${editing ? 'BÀI ĐĂNG CỦA TÔI' : 'THANH LÝ ĐỒ'}</p>
  <h1 class="page-title">${editing ? 'Sửa bài đăng' : 'Đăng bài mới'}</h1>
  <p class="page-subtitle">
    <c:choose>
      <c:when test="${editing and product.rejected}">Bài đã bị từ chối. Sửa lại rồi lưu, bài sẽ được gửi admin duyệt lại.</c:when>
      <c:when test="${editing}">Bài đã duyệt thì sửa xong vẫn hiện bình thường.</c:when>
      <c:otherwise>Bài sẽ hiện trên bảng tin sau khi admin duyệt. Mô tả trung thực giúp bán nhanh và tránh khiếu nại.</c:otherwise>
    </c:choose>
  </p>
</section>

<c:if test="${not empty error}">
  <div class="alert alert-error"><c:out value="${error}"/><c:if test="${not editing}"> Ảnh đã chọn cần chọn lại.</c:if></div>
</c:if>

<form class="product-form" method="post" enctype="multipart/form-data"
      action="${ctx}${editing ? '/product/edit' : '/product/new'}" data-max-images="${maxImages}">
  <c:if test="${editing}"><input type="hidden" name="id" value="${fn:escapeXml(product.id)}"></c:if>

  <%-- 1. Ảnh --%>
  <fieldset class="form-card">
    <legend class="form-card-title">Ảnh món đồ</legend>
    <p class="form-hint">Tối đa ${maxImages} ảnh PNG/JPEG, mỗi ảnh ≤ 5 MB. Ảnh đầu tiên là ảnh bìa.</p>
    <div class="photo-grid" id="photoGrid">
      <c:forEach var="img" items="${product.images}">
        <label class="photo-tile existing">
          <img src="${ctx}<c:out value='${img}'/>" alt="">
          <input type="checkbox" name="keepImage" value="${fn:escapeXml(img)}" checked>
          <span class="photo-remove" title="Bỏ ảnh này">✕</span>
        </label>
      </c:forEach>
      <label class="photo-add" id="photoAdd">
        <input type="file" name="photos" id="photoInput" accept="image/png,image/jpeg,image/gif" multiple ${editing ? '' : 'required'}>
        <span class="photo-add-icon">＋</span>
        <span>Thêm ảnh</span>
      </label>
    </div>
  </fieldset>

  <%-- 2. Thông tin món --%>
  <fieldset class="form-card">
    <legend class="form-card-title">Thông tin món đồ</legend>
    <label class="form-field">
      <span>Tên món <em>*</em></span>
      <input type="text" name="name" value="${fn:escapeXml(product.name)}" required minlength="3" maxlength="200"
             placeholder="Ví dụ: Bàn phím cơ Keychron K2, switch đỏ">
    </label>

    <div class="form-row">
      <label class="form-field">
        <span>Danh mục <em>*</em></span>
        <select name="categoryId" id="parentCategory" required>
          <option value="">Chọn danh mục</option>
          <c:forEach var="cg" items="${categories}">
            <c:if test="${cg.topLevel}">
              <option value="${fn:escapeXml(cg.id)}" ${cg.id == selectedParent ? 'selected' : ''}><c:out value="${cg.name}"/></option>
            </c:if>
          </c:forEach>
        </select>
      </label>
      <label class="form-field" id="childCategoryField">
        <span>Danh mục con</span>
        <select name="childCategoryId" id="childCategory">
          <option value="">Không chọn</option>
          <c:forEach var="cg" items="${categories}">
            <c:if test="${not cg.topLevel}">
              <option value="${fn:escapeXml(cg.id)}" data-parent="${fn:escapeXml(cg.parentCategoryId)}" ${cg.id == selectedChild ? 'selected' : ''}><c:out value="${cg.name}"/></option>
            </c:if>
          </c:forEach>
        </select>
      </label>
    </div>

    <div class="form-field">
      <span>Tình trạng <em>*</em></span>
      <div class="choice-row">
        <c:forEach var="cond" items="${['new', 'used', 'damaged']}" varStatus="st">
          <label class="choice">
            <input type="radio" name="condition" value="${cond}" ${product.condition.code == cond ? 'checked' : ''} required>
            <span>${st.index == 0 ? 'Mới' : (st.index == 1 ? 'Đã qua sử dụng' : 'Hỏng nhẹ')}</span>
          </label>
        </c:forEach>
      </div>
    </div>

    <label class="form-field">
      <span>Mô tả</span>
      <textarea name="description" rows="6" maxlength="5000" data-counter="descCount"
                placeholder="Đã dùng bao lâu, còn bảo hành không, có trầy xước hay lỗi gì, phụ kiện đi kèm..."><c:out value="${product.description}"/></textarea>
      <small class="char-count" id="descCount"></small>
    </label>
  </fieldset>

  <%-- 3. Bán hay đổi --%>
  <fieldset class="form-card">
    <legend class="form-card-title">Hình thức</legend>
    <div class="choice-row" id="listingChoices">
      <label class="choice choice-lg">
        <input type="radio" name="listingType" value="sale" ${product.listingType.code == 'sale' ? 'checked' : ''} required>
        <span><strong>Bán</strong><small>Nhận tiền</small></span>
      </label>
      <label class="choice choice-lg">
        <input type="radio" name="listingType" value="swap" ${product.listingType.code == 'swap' ? 'checked' : ''}>
        <span><strong>Trao đổi</strong><small>Đổi lấy món khác</small></span>
      </label>
      <label class="choice choice-lg">
        <input type="radio" name="listingType" value="both" ${product.listingType.code == 'both' ? 'checked' : ''}>
        <span><strong>Cả hai</strong><small>Bán hoặc đổi đều được</small></span>
      </label>
    </div>

    <div class="form-row" data-show-for="sale both">
      <label class="form-field">
        <span>Giá bán (đ) <em>*</em></span>
        <input type="text" inputmode="numeric" name="price" class="js-money" placeholder="Ví dụ: 1.500.000"
               value="<fmt:formatNumber value='${product.price}' pattern='#,##0'/>">
      </label>
      <label class="check-field">
        <input type="checkbox" name="acceptCod" ${product.acceptCod ? 'checked' : ''}>
        <span>Nhận thanh toán khi giao hàng (COD)</span>
      </label>
    </div>

    <div data-show-for="swap both">
      <label class="form-field">
        <span>Giá trị ước tính (đ) <em>*</em></span>
        <input type="text" inputmode="numeric" name="estimatedValue" class="js-money" placeholder="Dùng để gợi ý món có giá trị tương đương"
               value="<fmt:formatNumber value='${product.estimatedValue}' pattern='#,##0'/>">
      </label>
      <label class="form-field">
        <span>Món muốn đổi</span>
        <input type="text" name="desiredExchangeNote" maxlength="500" value="${fn:escapeXml(product.desiredExchangeNote)}"
               placeholder="Ví dụ: tai nghe chống ồn, chuột không dây">
      </label>
    </div>
  </fieldset>

  <div class="form-submit">
    <a class="btn btn-lg" href="${ctx}${editing ? '/product/mine' : '/home'}">Huỷ</a>
    <button class="btn btn-dark btn-lg" type="submit">${editing ? 'Lưu thay đổi' : 'Đăng bài'}</button>
  </div>
</form>
</div>

<script src="${ctx}/assets/js/product.js?v=${assetVer}" defer></script>
<%@ include file="/WEB-INF/views/common/footer.jsp" %>
