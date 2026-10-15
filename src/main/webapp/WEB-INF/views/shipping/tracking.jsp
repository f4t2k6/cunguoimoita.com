<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="pageTitle" value="Theo dõi đơn hàng"/>
<c:set var="pageCss" value="shipping.css"/>
<%@ include file="/WEB-INF/views/common/header.jsp" %>
<div class="shipping-page">
  <h1>Theo dõi đơn hàng</h1>

  <c:if test="${not empty error}"><p class="ship-error"><c:out value="${error}"/></p></c:if>

  <c:if test="${not empty shipment}">
    <c:set var="s" value="${shipment}"/>
    <section class="ship-card">
      <p>Mã đơn: <strong><c:out value="${empty orderCode ? s.orderId : orderCode}"/></strong></p>
      <p>Vận đơn: <strong><c:out value="${s.track()}"/></strong></p>
      <p>Phí vận chuyển: <fmt:formatNumber value="${s.shippingFee}" pattern="#,##0"/> đ</p>
    </section>

    <%-- Dòng thời gian: các bước đã qua được tô đậm --%>
    <ol class="ship-timeline">
      <c:forEach var="step" items="${steps}">
        <li class="${step.ordinal() <= s.status.ordinal() and s.status != 'RETURNED' ? 'done' : ''}">
          ${step.label()}
          <c:if test="${step == 'DELIVERED' and not empty s.deliveredDate}"> (${s.deliveredDate})</c:if>
        </li>
      </c:forEach>
      <c:if test="${s.status == 'RETURNED'}"><li class="done returned">Hoàn hàng</li></c:if>
    </ol>

    <c:if test="${s.cod}">
      <section class="ship-card">
        <h2>Thu hộ (COD): <fmt:formatNumber value="${s.codAmount}" pattern="#,##0"/> đ</h2>
        <p>Đã thu của người mua: ${empty s.codCollectedAt ? 'chưa' : s.codCollectedAt}</p>
        <p>Đã nộp về sàn: ${empty s.codRemittedAt ? 'chưa' : s.codRemittedAt}</p>
      </section>
    </c:if>

    <%-- Chỉ người bán của đơn (hoặc admin) mới thấy các nút; người mua chỉ xem --%>
    <c:if test="${canManage}">
    <section class="ship-actions">
      <c:if test="${s.status == 'CREATED'}">
        <form method="post">
          <input type="hidden" name="orderId" value="${fn:escapeXml(s.orderId)}">
          <input type="hidden" name="action" value="ship">
          <input name="trackingCode" required placeholder="Mã vận đơn">
          <button type="submit">Đã gửi hàng</button>
        </form>
      </c:if>

      <%-- Nút giả lập đơn vị vận chuyển cập nhật trạng thái --%>
      <c:if test="${s.status == 'PICKED_UP'}">
        <form method="post">
          <input type="hidden" name="orderId" value="${fn:escapeXml(s.orderId)}">
          <input type="hidden" name="action" value="status">
          <button name="status" value="in_transit">Giả lập: đang giao</button>
        </form>
      </c:if>
      <c:if test="${s.status == 'IN_TRANSIT'}">
        <form method="post">
          <input type="hidden" name="orderId" value="${fn:escapeXml(s.orderId)}">
          <input type="hidden" name="action" value="status">
          <button name="status" value="delivered">Giả lập: đã giao</button>
          <button name="status" value="returned" class="secondary">Giả lập: hoàn hàng</button>
        </form>
      </c:if>

      <c:if test="${s.codStatus == 'PENDING'}">
        <form method="post">
          <input type="hidden" name="orderId" value="${fn:escapeXml(s.orderId)}">
          <button name="action" value="cod_collected">Shipper đã thu tiền COD</button>
        </form>
      </c:if>
      <c:if test="${s.codStatus == 'COLLECTED'}">
        <form method="post">
          <input type="hidden" name="orderId" value="${fn:escapeXml(s.orderId)}">
          <button name="action" value="cod_remitted">Shipper đã nộp tiền về sàn</button>
        </form>
      </c:if>
    </section>
    </c:if>
  </c:if>
</div>
<%@ include file="/WEB-INF/views/common/footer.jsp" %>
