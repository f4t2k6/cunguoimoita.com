<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<fmt:setLocale value="vi_VN"/>
<%-- Đơn mua / đơn bán dạng thẻ (kiểu Shopee). Dữ liệu từ OrderServlet.showList: cards (List<OrderCard>), as (buyer|seller), message, error --%>
<c:set var="pageTitle" value="${as == 'seller' ? 'Đơn bán của tôi' : 'Đơn mua của tôi'}"/>
<c:set var="pageCss" value="order.css"/>
<c:set var="orderTab" value="${as == 'seller' ? 'seller' : 'buyer'}"/>
<%@ include file="/WEB-INF/views/common/header.jsp" %>
<div class="order-page page-card">
  <div class="page-card-head">
    <p class="page-eyebrow">ĐƠN HÀNG</p>
    <h1 class="page-title">${pageTitle}</h1>
  </div>
  <%@ include file="/WEB-INF/views/order/_tabs.jspf" %>

  <c:if test="${not empty message}"><p class="order-alert"><c:out value="${message}"/></p></c:if>
  <c:if test="${not empty error}"><p class="order-alert error"><c:out value="${error}"/></p></c:if>

  <c:choose>
    <c:when test="${empty cards}">
      <div class="cart-empty">
        <div class="cart-empty-icon">📦</div>
        <p>Chưa có đơn nào.</p>
      </div>
    </c:when>
    <c:otherwise>
      <div class="order-cards">
        <c:forEach var="card" items="${cards}">
          <c:set var="o" value="${card.order}"/>
          <c:url var="detailUrl" value="/order/detail"><c:param name="id" value="${o.id}"/></c:url>
          <article class="oc">
            <%-- Góc trái trên: người bán (ở Đơn mua) / người mua (ở Đơn bán) --%>
            <header class="oc-head">
              <span class="oc-partner-label">${as == 'seller' ? 'Người mua' : 'Người bán'}</span>
              <strong class="oc-partner"><c:out value="${empty card.partnerName ? 'Người dùng' : card.partnerName}"/></strong>
            </header>

            <a class="oc-items" href="${detailUrl}">
              <c:forEach var="line" items="${card.lines}">
                <div class="oc-line">
                  <c:choose>
                    <c:when test="${not empty line.image}"><img class="oc-thumb" src="${ctx}<c:out value='${line.image}'/>" alt="" loading="lazy"></c:when>
                    <c:otherwise><span class="oc-thumb oc-thumb-empty">📦</span></c:otherwise>
                  </c:choose>
                  <div class="oc-line-body">
                    <p class="oc-name"><c:out value="${line.name}"/></p>
                    <p class="oc-qty">x${line.quantity}</p>
                    <p class="oc-price"><fmt:formatNumber value="${line.unitPrice}" pattern="#,##0"/>đ</p>
                  </div>
                </div>
              </c:forEach>
            </a>

            <%-- Số tiền sản phẩm sau khi deal (chưa gồm phí vận chuyển) --%>
            <p class="oc-total">Tổng số tiền (${card.itemCount} sản phẩm): <strong><fmt:formatNumber value="${o.subTotal}" pattern="#,##0"/>đ</strong></p>

            <a class="oc-notice oc-notice-${card.noticeTone}" href="${detailUrl}">
              <span><c:out value="${card.notice}"/></span>
              <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M9 5l7 7-7 7" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
            </a>

            <div class="oc-actions">
              <c:choose>
                <%-- Người mua: đã chốt giá nhưng chưa thanh toán → Huỷ deal / Thanh toán --%>
                <c:when test="${as == 'buyer' and card.awaitingPayment}">
                  <form method="post" action="${ctx}/order/detail" onsubmit="return confirm('Huỷ deal đơn ${o.code}? Món hàng sẽ được mở bán lại.');">
                    <input type="hidden" name="id" value="${fn:escapeXml(o.id)}">
                    <input type="hidden" name="action" value="cancel">
                    <input type="hidden" name="reason" value="Người mua huỷ deal">
                    <input type="hidden" name="back" value="list">
                    <button type="submit" class="oc-btn">Huỷ deal</button>
                  </form>
                  <c:choose>
                    <c:when test="${o.status == 'AWAITING_PAYMENT'}"><a class="oc-btn oc-btn-main" href="${ctx}/payment/checkout?orderId=${fn:escapeXml(o.id)}">Thanh toán</a></c:when>
                    <c:otherwise><a class="oc-btn oc-btn-main" href="${detailUrl}">Thanh toán</a></c:otherwise>
                  </c:choose>
                </c:when>
                <%-- Người mua: hàng đã giao → Trả hàng/Hoàn tiền / Đã nhận được hàng --%>
                <c:when test="${as == 'buyer' and o.status == 'DELIVERED'}">
                  <a class="oc-btn" href="${ctx}/dispute?orderId=${fn:escapeXml(o.id)}">Trả hàng/Hoàn tiền</a>
                  <form method="post" action="${ctx}/order/detail">
                    <input type="hidden" name="id" value="${fn:escapeXml(o.id)}">
                    <input type="hidden" name="action" value="received">
                    <input type="hidden" name="back" value="list">
                    <button type="submit" class="oc-btn oc-btn-main">Đã nhận được hàng</button>
                  </form>
                </c:when>
                <c:otherwise>
                  <a class="oc-btn" href="${detailUrl}">Xem chi tiết</a>
                </c:otherwise>
              </c:choose>
            </div>
          </article>
        </c:forEach>
      </div>
    </c:otherwise>
  </c:choose>
</div>
<%@ include file="/WEB-INF/views/common/footer.jsp" %>
