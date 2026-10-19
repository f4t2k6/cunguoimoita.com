<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%-- Trang giỏ hàng kiểu Shopee. Dữ liệu từ CartServlet: lines (List<CartLine>, đã sắp theo người bán), total, error, message.
     Tick chọn từng món / cả shop / "Chọn tất cả" → tổng tiền và số món chỉ tính các món đã chọn (cart.js trong trang).
     "Mua hàng" gửi các productId đã tick sang /order/checkout; món không chọn vẫn ở lại giỏ. --%>
<c:set var="pageTitle" value="Giỏ hàng"/>
<c:set var="pageCss" value="order.css"/>
<c:set var="orderTab" value="cart"/>
<%@ include file="/WEB-INF/views/common/header.jsp" %>
<div class="order-page page-card cart-page">
  <div class="page-card-head">
    <p class="page-eyebrow">MUA SẮM</p>
    <h1 class="page-title">Giỏ hàng</h1>
  </div>
  <%@ include file="/WEB-INF/views/order/_tabs.jspf" %>

  <%-- Thông báo lỗi / thành công gửi về qua URL (?error=... / ?message=...) --%>
  <c:if test="${not empty error}"><p class="order-alert error"><c:out value="${error}"/></p></c:if>
  <c:if test="${not empty message}"><p class="order-alert"><c:out value="${message}"/></p></c:if>

  <c:choose>
    <c:when test="${empty lines}">
      <div class="cart-empty">
        <div class="cart-empty-icon">🛒</div>
        <p>Giỏ hàng của bạn còn trống.</p>
        <a class="btn-link" href="${ctx}/home">Tiếp tục mua sắm</a>
      </div>
    </c:when>
    <c:otherwise>
      <%-- Mỗi nút "Xoá" là 1 form riêng (form không lồng trong form chọn món); nút trong bảng trỏ tới bằng thuộc tính form= --%>
      <c:forEach var="line" items="${lines}">
        <form id="rm-${line.productId()}" method="post" action="${ctx}/order/cart" hidden>
          <input type="hidden" name="action" value="remove">
          <input type="hidden" name="productId" value="${fn:escapeXml(line.productId())}">
        </form>
      </c:forEach>

      <form id="cartForm" method="post" action="${ctx}/order/checkout">
        <div class="cart-head-row">
          <label class="cart-check"><input type="checkbox" class="js-check-all"> <span>Sản phẩm</span></label>
          <span class="cart-col-price">Đơn giá</span>
          <span class="cart-col-state">Tình trạng</span>
          <span class="cart-col-action">Thao tác</span>
        </div>

        <c:forEach var="line" items="${lines}" varStatus="st">
          <%-- lines đã sắp theo người bán: sang người bán mới thì mở 1 khối shop mới --%>
          <c:if test="${st.first or line.sellerId() != lines[st.index - 1].sellerId()}">
            <c:if test="${not st.first}"></div></section></c:if>
            <section class="cart-shop" data-seller="${fn:escapeXml(line.sellerId())}">
              <div class="cart-shop-head">
                <label class="cart-check"><input type="checkbox" class="js-check-shop" aria-label="Chọn tất cả món của shop"></label>
                <span class="cart-shop-icon" aria-hidden="true">🏪</span>
                <a class="cart-shop-name" href="${ctx}/users/profile?id=${fn:escapeXml(line.sellerId())}"><c:out value="${line.sellerName()}"/></a>
              </div>
              <div class="cart-shop-items">
          </c:if>

          <div class="cart-item ${line.available() ? '' : 'is-off'}">
            <label class="cart-check">
              <input type="checkbox" name="productId" value="${fn:escapeXml(line.productId())}" class="js-check-item"
                     data-price="${line.price()}" ${line.available() ? '' : 'disabled'}
                     aria-label="Chọn <c:out value='${line.productName()}'/>">
            </label>
            <a class="cart-item-main" href="${ctx}/product?id=${fn:escapeXml(line.productId())}">
              <c:choose>
                <c:when test="${not empty line.coverImage()}"><img class="cart-thumb" src="${ctx}<c:out value='${line.coverImage()}'/>" alt="" loading="lazy"></c:when>
                <c:otherwise><span class="cart-thumb cart-thumb-empty">Chưa có ảnh</span></c:otherwise>
              </c:choose>
              <span class="cart-item-name"><c:out value="${line.productName()}"/></span>
            </a>
            <span class="cart-col-price"><fmt:formatNumber value="${line.price()}" pattern="#,##0"/> đ</span>
            <span class="cart-col-state">
              <c:choose>
                <c:when test="${line.available()}"><span class="cart-state-ok">Còn hàng</span></c:when>
                <c:when test="${line.productStatus() == 'reserved'}"><span class="tag-off">Đã có người đặt</span></c:when>
                <c:otherwise><span class="tag-off">Đã bán / ngừng bán</span></c:otherwise>
              </c:choose>
            </span>
            <span class="cart-col-action">
              <button type="submit" form="rm-${line.productId()}" class="cart-remove">Xoá</button>
            </span>
          </div>
          <c:if test="${st.last}"></div></section></c:if>
        </c:forEach>

        <%-- Thanh tổng tiền dính đáy, giống Shopee --%>
        <div class="cart-bar">
          <label class="cart-check"><input type="checkbox" class="js-check-all"> <span>Chọn tất cả (<span class="js-selectable">0</span>)</span></label>
          <button type="submit" class="cart-bar-link" formaction="${ctx}/order/cart" name="action" value="removeSelected" disabled>Xoá mục đã chọn</button>
          <div class="cart-bar-total">
            <span>Tổng cộng (<span class="js-count">0</span> sản phẩm):</span>
            <strong class="js-total">0 đ</strong>
          </div>
          <button type="submit" class="cart-buy js-buy" disabled>Mua hàng</button>
        </div>
        <p class="hint cart-hint">Mỗi người bán sẽ tạo 1 đơn riêng. Phí vận chuyển và voucher chọn ở bước tiếp theo.</p>
      </form>

      <script>
        // Tick chọn món: cập nhật ô shop, ô "Chọn tất cả", số món và tổng tiền các món đã chọn
        (function () {
          const form = document.getElementById('cartForm');
          const items = () => [...form.querySelectorAll('.js-check-item:not(:disabled)')];
          const fmt = n => n.toLocaleString('vi-VN') + ' đ';
          function refresh() {
            const all = items();
            const picked = all.filter(i => i.checked);
            const total = picked.reduce((s, i) => s + Number(i.dataset.price), 0);
            form.querySelector('.js-count').textContent = picked.length;
            form.querySelector('.js-selectable').textContent = all.length;
            form.querySelector('.js-total').textContent = fmt(total);
            form.querySelector('.js-buy').disabled = picked.length === 0;
            form.querySelector('.cart-bar-link').disabled = picked.length === 0;
            form.querySelectorAll('.js-check-all').forEach(c => {
              c.checked = all.length > 0 && picked.length === all.length;
              c.indeterminate = picked.length > 0 && picked.length < all.length;
              c.disabled = all.length === 0;
            });
            form.querySelectorAll('.cart-shop').forEach(shop => {
              const own = [...shop.querySelectorAll('.js-check-item:not(:disabled)')];
              const box = shop.querySelector('.js-check-shop');
              const n = own.filter(i => i.checked).length;
              box.checked = own.length > 0 && n === own.length;
              box.indeterminate = n > 0 && n < own.length;
              box.disabled = own.length === 0;
              shop.classList.toggle('is-picked', n > 0);
            });
            form.querySelectorAll('.cart-item').forEach(row => {
              const box = row.querySelector('.js-check-item');
              row.classList.toggle('is-picked', box.checked && !box.disabled);
            });
          }
          form.addEventListener('change', e => {
            const t = e.target;
            if (t.classList.contains('js-check-all')) {
              items().forEach(i => { i.checked = t.checked; });
            } else if (t.classList.contains('js-check-shop')) {
              t.closest('.cart-shop').querySelectorAll('.js-check-item:not(:disabled)').forEach(i => { i.checked = t.checked; });
            }
            refresh();
          });
          refresh();
        })();
      </script>
    </c:otherwise>
  </c:choose>
</div>
<%@ include file="/WEB-INF/views/common/footer.jsp" %>
