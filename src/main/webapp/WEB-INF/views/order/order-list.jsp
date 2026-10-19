<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%-- Đơn mua / đơn bán. Dữ liệu từ OrderServlet.showList: orders (List<Order>), as (buyer|seller), message --%>
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

  <c:choose>
    <c:when test="${empty orders}">
      <div class="cart-empty">
        <div class="cart-empty-icon">📦</div>
        <p>Chưa có đơn nào.</p>
      </div>
    </c:when>
    <c:otherwise>
      <div class="table-scroll"><table class="order-table">
        <tr><th>Mã đơn</th><th>Ngày đặt</th><th>Tổng tiền</th><th>Thanh toán</th><th>Trạng thái</th><th>Thao tác</th></tr>
        <c:forEach var="o" items="${orders}">
          <tr>
            <td><strong><c:out value="${o.code}"/></strong></td>
            <td>${o.orderDate.toLocalDate()}</td>
            <td><fmt:formatNumber value="${o.totalAmount}" pattern="#,##0"/> đ</td>
            <td>${o.paymentMethod == 'ONLINE' ? 'Online' : 'COD'}</td>
            <td><span class="status status-${o.status}">${o.status.label()}</span></td>
            <td>
              <a href="${ctx}/order/detail?id=${o.id}">Xem</a>
              <c:if test="${as == 'buyer' and o.status == 'AWAITING_PAYMENT'}">
                · <a href="${ctx}/payment/checkout?orderId=${o.id}"><strong>Thanh toán</strong></a>
              </c:if>
              <c:if test="${as == 'buyer' and o.status == 'DELIVERED'}">
                · <a href="${ctx}/dispute?orderId=${o.id}">Trả hàng/Hoàn tiền</a>
              </c:if>
            </td>
          </tr>
        </c:forEach>
      </table></div>
    </c:otherwise>
  </c:choose>
</div>
<%@ include file="/WEB-INF/views/common/footer.jsp" %>