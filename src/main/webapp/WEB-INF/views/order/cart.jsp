<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%-- Trang giỏ hàng. Dữ liệu từ CartServlet: lines (List<CartLine>), total, error, message --%>
<c:set var="pageTitle" value="Giỏ hàng"/>
<c:set var="pageCss" value="order.css"/>
<%@ include file="/WEB-INF/views/common/header.jsp" %>
<div class="order-page">
    <h1>Giỏ hàng</h1>
    <%-- Lối vào danh sách đơn (header chung chưa có mục này) --%>
    <nav class="order-tabs">
      <a href="${ctx}/order/cart" class="active">Giỏ hàng</a>
      <a href="${ctx}/order/list?as=buyer">Đơn mua</a>
      <a href="${ctx}/order/list?as=seller">Đơn bán</a>
      <a href="${ctx}/review/tier">Hạng thành viên</a>
    </nav>

  <%-- Thông báo lỗi / thành công gửi về qua URL (?error=... / ?message=...) --%>
  <c:if test="${not empty error}"><p class="order-alert error"><c:out value="${error}"/></p></c:if>
  <c:if test="${not empty message}"><p class="order-alert"><c:out value="${message}"/></p></c:if>

  <c:choose>
    <c:when test="${empty lines}">
      <p class="order-card">Giỏ hàng đang trống. <a href="${ctx}/home">Tiếp tục mua sắm</a></p>
    </c:when>
    <c:otherwise>
      <table class="order-table">
        <tr><th>Sản phẩm</th><th>Giá</th><th>Tình trạng</th><th></th></tr>
        <c:forEach var="line" items="${lines}" varStatus="st">
          <%-- lines đã sắp theo người bán: sang người bán mới thì in 1 dòng tên shop --%>
          <c:if test="${st.first or line.sellerId() != lines[st.index - 1].sellerId()}">
            <tr class="seller-row"><td colspan="4">Người bán: <c:out value="${line.sellerName()}"/></td></tr>
          </c:if>
          <tr>
            <td><c:out value="${line.productName()}"/></td>
            <td><fmt:formatNumber value="${line.price()}" pattern="#,##0"/> đ</td>
            <td>
              <c:choose>
                <c:when test="${line.productStatus() == 'available'}">Còn hàng</c:when>
                <c:otherwise><span class="tag-off">Đã có người đặt</span></c:otherwise>
              </c:choose>
            </td>
            <td>
              <%-- Mỗi nút là 1 form nhỏ: gửi action + productId cho CartServlet.doPost --%>
              <form method="post" action="${ctx}/order/cart">
                <input type="hidden" name="action" value="remove">
                <input type="hidden" name="productId" value="${line.productId()}">
                <button class="secondary">Xoá</button>
              </form>
            </td>
          </tr>
        </c:forEach>
      </table>

      <div class="order-summary">
        <p>Tạm tính: <strong><fmt:formatNumber value="${total}" pattern="#,##0"/> đ</strong></p>
        <p class="hint">Mỗi người bán sẽ tạo 1 đơn riêng. Phí vận chuyển và voucher chọn ở bước tiếp theo.</p>
        <form method="post" action="${ctx}/order/cart" class="inline">
          <input type="hidden" name="action" value="clear">
          <button class="secondary">Xoá cả giỏ</button>
        </form>
        <form method="post" action="${ctx}/order/checkout" class="inline">
          <button>Đặt hàng</button>
        </form>
      </div>
    </c:otherwise>
  </c:choose>
</div>
<%@ include file="/WEB-INF/views/common/footer.jsp" %>