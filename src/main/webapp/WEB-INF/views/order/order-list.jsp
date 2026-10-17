<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%-- Đơn mua / đơn bán. Dữ liệu từ OrderServlet.showList: orders (List<Order>), as (buyer|seller), message --%>
<c:set var="pageTitle" value="${as == 'seller' ? 'Đơn bán của tôi' : 'Đơn mua của tôi'}"/>
<c:set var="pageCss" value="order.css"/>
<%@ include file="/WEB-INF/views/common/header.jsp" %>
<div class="order-page">
  <h1>${pageTitle}</h1>

  <%-- 2 tab: đang ở tab nào thì tab đó có class "active" --%>
  <nav class="order-tabs">
    <a href="${ctx}/order/cart">Giỏ hàng</a>
    <a href="${ctx}/order/list?as=buyer" class="${as == 'buyer' ? 'active' : ''}">Đơn mua</a>
    <a href="${ctx}/order/list?as=seller" class="${as == 'seller' ? 'active' : ''}">Đơn bán</a>
    <a href="${ctx}/review/tier">Hạng thành viên</a>
  </nav>

  <c:if test="${not empty message}"><p class="order-alert"><c:out value="${message}"/></p></c:if>

  <c:choose>
    <c:when test="${empty orders}">
      <p class="order-card">Chưa có đơn nào.</p>
    </c:when>
    <c:otherwise>
      <table class="order-table">
        <tr><th>Mã đơn</th><th>Ngày đặt</th><th>Tổng tiền</th><th>Thanh toán</th><th>Trạng thái</th><th></th></tr>
        <c:forEach var="o" items="${orders}">
          <tr>
            <td><strong><c:out value="${o.code}"/></strong></td>
            <td>${o.orderDate.toLocalDate()}</td>
            <td><fmt:formatNumber value="${o.totalAmount}" pattern="#,##0"/> đ</td>
            <td>${o.paymentMethod == 'ONLINE' ? 'Online' : 'COD'}</td>
            <td><span class="status status-${o.status}">${o.status.label()}</span></td>
            <td><a href="${ctx}/order/detail?id=${o.id}">Xem</a></td>
          </tr>
        </c:forEach>
      </table>
    </c:otherwise>
  </c:choose>
</div>
<%@ include file="/WEB-INF/views/common/footer.jsp" %>