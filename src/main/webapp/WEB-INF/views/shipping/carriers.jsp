<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="pageTitle" value="Đơn vị vận chuyển"/>
<c:set var="pageCss" value="shipping.css"/>
<%@ include file="/WEB-INF/views/common/header.jsp" %>
<div class="shipping-page page-card">
  <div class="page-card-head">
    <p class="page-eyebrow">VẬN CHUYỂN</p>
    <h1 class="page-title">Đơn vị vận chuyển</h1>
  </div>

  <c:if test="${not empty error}"><p class="ship-error"><c:out value="${error}"/></p></c:if>

  <form class="ship-form-inline" method="get">
    <label>Gửi từ <input name="from" value="${fn:escapeXml(from)}" placeholder="VD: Hà Nội"></label>
    <label>Giao tới <input name="to" value="${fn:escapeXml(to)}" placeholder="VD: TP.HCM"></label>
    <button type="submit">Tính phí</button>
  </form>

  <table class="ship-table">
    <thead>
    <tr><th>Đơn vị</th><th>Hotline</th><th>Phí cơ bản</th><th>Phí cho tuyến này</th><th>Dự kiến</th></tr>
    </thead>
    <tbody>
    <c:forEach var="carrier" items="${carriers}">
      <tr>
        <td><strong><c:out value="${carrier.name}"/></strong></td>
        <td><c:out value="${carrier.hotline}"/></td>
        <td><fmt:formatNumber value="${carrier.baseFee}" pattern="#,##0"/> đ</td>
        <td>
          <c:choose>
            <c:when test="${not empty from and not empty to}">
              <fmt:formatNumber value="${carrier.calculateFee(from, to)}" pattern="#,##0"/> đ
            </c:when>
            <c:otherwise>–</c:otherwise>
          </c:choose>
        </td>
        <td>${carrier.estimatedDays} ngày</td>
      </tr>
    </c:forEach>
    </tbody>
  </table>

  <p class="hint">Người mua chọn đơn vị vận chuyển ngay trong trang chi tiết đơn hàng, phí ship được cộng vào tổng trước khi thanh toán.</p>
</div>
<%@ include file="/WEB-INF/views/common/footer.jsp" %>
