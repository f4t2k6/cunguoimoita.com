<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN" scope="request"/>
<%-- Lịch sử thanh toán (mở từ Cài đặt). Dữ liệu từ PaymentHistoryServlet: rows (payment, orderCode) --%>
<c:set var="pageTitle" value="Lịch sử thanh toán"/>
<c:set var="pageCss" value="order.css"/>
<%@ include file="/WEB-INF/views/common/header.jsp" %>
<div class="order-page page-card">
  <div class="page-card-head">
    <p class="page-eyebrow">THANH TOÁN</p>
    <h1 class="page-title">Lịch sử thanh toán</h1>
  </div>
  <p class="hint">Các lần bạn thanh toán online cho đơn mua (VNPay, MoMo, chuyển khoản). Đơn trả khi nhận hàng (COD) không có ở đây.</p>

  <c:choose>
    <c:when test="${empty rows}">
      <div class="cart-empty">
        <div class="cart-empty-icon">💳</div>
        <p>Bạn chưa có giao dịch thanh toán nào.</p>
      </div>
    </c:when>
    <c:otherwise>
      <div class="table-scroll">
        <table class="order-table pay-table">
          <tr><th>Mã giao dịch</th><th>Đơn hàng</th><th>Cổng</th><th>Số tiền</th><th>Trạng thái</th><th>Thời gian</th></tr>
          <c:forEach var="r" items="${rows}">
            <c:set var="p" value="${r.payment()}"/>
            <tr>
              <td><strong><c:out value="${p.code}"/></strong></td>
              <td><a href="${ctx}/order/detail?id=${p.orderId}"><c:out value="${r.orderCode()}"/></a></td>
              <td>${p.gateway.displayName}</td>
              <td><fmt:formatNumber value="${p.amount}" pattern="#,##0"/> đ</td>
              <td><span class="pay-status pay-${p.status}">${p.status.displayName}</span></td>
              <td>${empty p.paidAt ? p.createdAt.toLocalDate() : p.paidAt.toLocalDate()}</td>
            </tr>
          </c:forEach>
        </table>
      </div>
    </c:otherwise>
  </c:choose>
</div>
<%@ include file="/WEB-INF/views/common/footer.jsp" %>
