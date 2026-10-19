<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN" scope="request"/>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%-- Thu nhập người bán (F9). Dữ liệu từ SellerPayoutServlet: payouts, summary, info --%>
<c:set var="pageTitle" value="Thu nhập của tôi"/>
<c:set var="pageCss" value="order.css"/>
<%@ include file="/WEB-INF/views/common/header.jsp" %>
<div class="order-page">
  <h1>Thu nhập của tôi</h1>

  <section class="order-card">
    <table class="money">
      <tr><td>Tổng tiền hàng đã bán</td><td><fmt:formatNumber value="${summary.totalGross()}" pattern="#,##0"/> đ</td></tr>
      <tr><td>Phí sàn</td><td>− <fmt:formatNumber value="${summary.totalCommission()}" pattern="#,##0"/> đ</td></tr>
      <tr class="total"><td>Thực nhận</td><td><fmt:formatNumber value="${summary.totalNet()}" pattern="#,##0"/> đ</td></tr>
      <tr><td>Đã chuyển</td><td><fmt:formatNumber value="${summary.completedAmount()}" pattern="#,##0"/> đ</td></tr>
      <tr><td>Đang chờ chuyển</td><td><fmt:formatNumber value="${summary.scheduledAmount()}" pattern="#,##0"/> đ</td></tr>
    </table>
    <c:if test="${not empty info}">
      <p class="hint">Hạng <strong>${info.tierName()}</strong>: phí sàn
        <fmt:formatNumber value="${info.commissionRate() * 100}" pattern="0.##"/>% tiền hàng (cơ bản 5%, hạng càng cao càng được giảm).</p>
      <c:choose>
        <c:when test="${empty info.payoutAccount()}">
          <p class="order-alert error">Bạn chưa khai tài khoản nhận tiền nên các khoản đang chờ chưa chuyển được.
            <a href="${ctx}/account/profile">Khai trong Hồ sơ</a></p>
        </c:when>
        <c:otherwise><p class="hint">Tài khoản nhận tiền: <c:out value="${info.payoutAccount()}"/></p></c:otherwise>
      </c:choose>
    </c:if>
    <p class="hint">Tiền của mỗi đơn được chuyển khi người mua bấm "Đã nhận hàng", hoặc tự động sau 7 ngày kể từ khi giao nếu không có khiếu nại.</p>
    <p><a class="btn-link" href="${ctx}/seller/payouts/export">Tải file CSV</a></p>
  </section>

  <section class="order-card">
    <h2>Các khoản theo đơn</h2>
    <c:choose>
      <c:when test="${empty payouts}"><p class="hint">Chưa có đơn nào hoàn tất.</p></c:when>
      <c:otherwise>
        <table class="order-table">
          <tr><th>Đơn</th><th>Tiền hàng</th><th>Phí sàn</th><th>Thực nhận</th><th>Trạng thái</th><th>Ngày chuyển</th></tr>
          <c:forEach var="p" items="${payouts}">
            <tr>
              <td><a href="${ctx}/order/detail?id=${p.orderId}">ORD-${fn:toUpperCase(fn:substring(p.orderId, 0, 8))}</a></td>
              <td><fmt:formatNumber value="${p.grossAmount}" pattern="#,##0"/> đ</td>
              <td><fmt:formatNumber value="${p.commissionFee}" pattern="#,##0"/> đ</td>
              <td><strong><fmt:formatNumber value="${p.netAmount}" pattern="#,##0"/> đ</strong></td>
              <td>${p.status.displayName}</td>
              <td>${empty p.payoutDate ? '–' : p.payoutDate.toLocalDate()}</td>
            </tr>
          </c:forEach>
        </table>
      </c:otherwise>
    </c:choose>
  </section>
</div>
<%@ include file="/WEB-INF/views/common/footer.jsp" %>
