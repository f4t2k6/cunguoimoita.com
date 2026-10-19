<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN" scope="request"/>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%-- Cổng thanh toán giả lập (F5), dùng khi chưa có khoá VNPay/MoMo thật. Dữ liệu từ PaymentCallbackServlet: payment --%>
<c:set var="pageTitle" value="Cổng thanh toán (giả lập)"/>
<c:set var="pageCss" value="order.css"/>
<c:set var="hideBack" value="${true}"/>
<%@ include file="/WEB-INF/views/common/header.jsp" %>
<div class="order-page page-card">
  <div class="page-card-head">
    <p class="page-eyebrow">THANH TOÁN</p>
    <h1 class="page-title">Cổng ${payment.gateway.displayName} <span class="status">giả lập</span></h1>
  </div>
  <section class="order-card">
    <p>Mã giao dịch: <strong><c:out value="${payment.code}"/></strong></p>
    <p>Số tiền: <strong><fmt:formatNumber value="${payment.amount}" pattern="#,##0"/> đ</strong></p>
    <p class="hint">Trang này đóng vai cổng thanh toán. Bấm một trong hai nút để giả lập kết quả cổng trả về.</p>
    <form method="post" action="${ctx}/payment/sandbox" class="actions">
      <input type="hidden" name="paymentId" value="${fn:escapeXml(payment.id)}">
      <button name="result" value="success">Thanh toán thành công</button>
      <button name="result" value="fail" class="secondary">Thanh toán thất bại</button>
    </form>
  </section>
</div>
<%@ include file="/WEB-INF/views/common/footer.jsp" %>
