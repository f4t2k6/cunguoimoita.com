<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN" scope="request"/>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%-- Chọn cổng thanh toán online (F5). Dữ liệu từ PaymentCheckoutServlet: order, gateways, vnpayReal, error.
     Số tiền chỉ để hiển thị: server luôn lấy tổng đơn trong DB khi tạo thanh toán. --%>
<c:set var="pageTitle" value="Thanh toán đơn ${order.code}"/>
<c:set var="pageCss" value="order.css"/>
<c:set var="backUrl" value="/order/detail?id=${order.id}"/>
<%@ include file="/WEB-INF/views/common/header.jsp" %>
<div class="order-page page-card">
  <div class="page-card-head">
    <p class="page-eyebrow">THANH TOÁN</p>
    <h1 class="page-title">Thanh toán đơn <c:out value="${order.code}"/></h1>
  </div>
  <c:if test="${not empty error}"><p class="order-alert error"><c:out value="${error}"/></p></c:if>

  <section class="order-card">
    <table class="money">
      <tr><td>Tiền hàng</td><td><fmt:formatNumber value="${order.subTotal}" pattern="#,##0"/> đ</td></tr>
      <tr><td>Phí vận chuyển</td><td><fmt:formatNumber value="${order.shippingFee}" pattern="#,##0"/> đ</td></tr>
      <tr><td>Giảm giá</td><td>− <fmt:formatNumber value="${order.discountAmount}" pattern="#,##0"/> đ</td></tr>
      <tr class="total"><td>Cần thanh toán</td><td><fmt:formatNumber value="${order.totalAmount}" pattern="#,##0"/> đ</td></tr>
    </table>
    <p class="hint">Tiền được sàn giữ hộ (Escrow) và chỉ chuyển cho người bán khi bạn xác nhận đã nhận hàng
      hoặc sau 7 ngày kể từ khi giao mà không có khiếu nại.</p>
  </section>

  <section class="order-card">
    <h2>Chọn cổng thanh toán</h2>
    <form method="post" action="${ctx}/payment/checkout" class="stack-form">
      <input type="hidden" name="orderId" value="${fn:escapeXml(order.id)}">
      <c:forEach var="g" items="${gateways}" varStatus="st">
        <label class="choice">
          <input type="radio" name="gateway" value="${g.dbValue()}" ${st.first ? 'checked' : ''}>
          ${g.displayName}
        </label>
      </c:forEach>
      <p class="hint">⚡ Cổng VNPay sẽ chuyển hướng trực tiếp sang giao diện thanh toán Sandbox chính thức của VNPay để bạn quét mã QR hoặc nhập thẻ ngân hàng thử nghiệm.</p>
      <button>Thanh toán <fmt:formatNumber value="${order.totalAmount}" pattern="#,##0"/> đ</button>
    </form>
  </section>
</div>
<%@ include file="/WEB-INF/views/common/footer.jsp" %>
