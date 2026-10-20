<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN" scope="request"/>
<%-- Kết quả thanh toán, chỉ hiển thị. Dữ liệu từ PaymentCallbackServlet: payment, payStatus --%>
<c:set var="pageTitle" value="Kết quả thanh toán"/>
<c:set var="pageCss" value="order.css"/>
<c:set var="backUrl" value="/order/detail?id=${payment.orderId}"/>
<%@ include file="/WEB-INF/views/common/header.jsp" %>
<div class="order-page page-card">
  <c:choose>
    <c:when test="${payStatus == 'paid'}"><h1 class="page-title">Thanh toán thành công ✔</h1></c:when>
    <c:when test="${payStatus == 'pending'}"><h1 class="page-title">Đang chờ cổng thanh toán xác nhận</h1></c:when>
    <c:when test="${payStatus == 'refunded'}"><h1 class="page-title">Khoản thanh toán đã được hoàn</h1></c:when>
    <c:otherwise><h1 class="page-title">Thanh toán không thành công</h1></c:otherwise>
  </c:choose>
  <section class="order-card">
    <p>Mã giao dịch: <strong><c:out value="${payment.code}"/></strong> · ${payment.gateway.displayName}</p>
    <p>Số tiền: <strong><fmt:formatNumber value="${payment.amount}" pattern="#,##0"/> đ</strong></p>
    <c:if test="${not empty payment.paidAt}"><p>Thời gian: ${payment.paidAt.toLocalDate()} ${payment.paidAt.toLocalTime().withNano(0)}</p></c:if>
    <c:if test="${payStatus == 'failed'}">
      <p class="hint">Có thể giao dịch bị huỷ, hoặc tổng đơn đã đổi (voucher, phí ship) sau khi bạn bắt đầu thanh toán. Hãy thanh toán lại.</p>
    </c:if>
    <p class="actions">
      <a class="btn-link" href="${ctx}/order/detail?id=${payment.orderId}">Xem đơn hàng</a>
      <c:if test="${payStatus == 'failed'}">
        <a class="btn-link" href="${ctx}/payment/checkout?orderId=${payment.orderId}">Thanh toán lại</a>
      </c:if>
    </p>
  </section>
</div>
<%@ include file="/WEB-INF/views/common/footer.jsp" %>
