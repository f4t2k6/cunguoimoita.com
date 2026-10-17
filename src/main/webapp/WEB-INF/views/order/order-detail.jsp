<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%-- Chi tiết + thanh toán 1 đơn. Dữ liệu từ OrderServlet.showDetail:
     order, isBuyer, isSeller, addresses (chỉ khi là người mua), error, message --%>
<c:set var="pageTitle" value="Đơn ${order.code}"/>
<c:set var="pageCss" value="order.css"/>
<%@ include file="/WEB-INF/views/common/header.jsp" %>
<c:set var="o" value="${order}"/>
<%-- Đơn còn sửa được (chưa thanh toán / chưa giao)? Giống luật requireEditable() trong Order --%>
<c:set var="editable" value="${o.status == 'NEW' or o.status == 'AWAITING_PAYMENT'}"/>
<div class="order-page">
  <h1>Đơn <c:out value="${o.code}"/> <span class="status status-${o.status}">${o.status.label()}</span></h1>
  <p class="hint">
    Ngày đặt: ${o.orderDate.toLocalDate()} ·
    Bạn là <strong>${isBuyer ? 'người mua' : 'người bán'}</strong> ·
    <a href="${ctx}/order/list?as=${isBuyer ? 'buyer' : 'seller'}">← Danh sách đơn</a>
  </p>

  <c:if test="${not empty error}"><p class="order-alert error"><c:out value="${error}"/></p></c:if>
  <c:if test="${not empty message}"><p class="order-alert"><c:out value="${message}"/></p></c:if>

  <%-- ========== CÁC MÓN + TIỀN ========== --%>
  <section class="order-card">
    <h2>Sản phẩm</h2>
    <table class="order-table">
      <tr><th>Mã sản phẩm</th><th>Số lượng</th><th>Giá chốt</th></tr>
      <c:forEach var="item" items="${o.items}">
        <tr>
          <td><c:out value="${item.productId}"/></td>
          <td>${item.quantity}</td>
          <td><fmt:formatNumber value="${item.unitPrice}" pattern="#,##0"/> đ</td>
        </tr>
      </c:forEach>
    </table>
    <table class="money">
      <tr><td>Tiền hàng</td><td><fmt:formatNumber value="${o.subTotal}" pattern="#,##0"/> đ</td></tr>
      <tr><td>Phí vận chuyển</td><td><fmt:formatNumber value="${o.shippingFee}" pattern="#,##0"/> đ</td></tr>
      <tr><td>Giảm giá</td><td>− <fmt:formatNumber value="${o.discountAmount}" pattern="#,##0"/> đ</td></tr>
      <tr class="total"><td>Tổng thanh toán</td><td><fmt:formatNumber value="${o.totalAmount}" pattern="#,##0"/> đ</td></tr>
    </table>
  </section>

  <%-- ========== TRANG THANH TOÁN: chỉ người mua, chỉ khi đơn còn sửa được ========== --%>
  <c:if test="${isBuyer and editable}">
    <section class="order-card">
      <h2>Thanh toán</h2>

      <%-- 1. Địa chỉ giao: chọn từ sổ địa chỉ (F1) --%>
      <form method="post" action="${ctx}/order/detail">
        <input type="hidden" name="id" value="${o.id}">
        <input type="hidden" name="action" value="address">
        <label>Địa chỉ giao
          <select name="addressId">
            <c:forEach var="a" items="${addresses}">
              <option value="${a.id}" ${a.id == o.shippingAddressId ? 'selected' : ''}>
                <c:out value="${a.label}: ${a.addressLine}, ${a.district}, ${a.province}"/>
              </option>
            </c:forEach>
          </select>
        </label>
        <button class="secondary">Lưu địa chỉ</button>
      </form>

      <%-- 2. Vận chuyển: trang của F6 (Thành) --%>
      <p>Vận chuyển: <a href="${ctx}/shipping/carriers">Xem đơn vị vận chuyển và phí ship</a></p>

      <%-- 3. Voucher: chỉ hiện ô nhập khi đơn chưa áp voucher --%>
      <c:choose>
        <c:when test="${empty o.voucherId}">
          <form method="post" action="${ctx}/order/detail">
            <input type="hidden" name="id" value="${o.id}">
            <input type="hidden" name="action" value="voucher">
            <label>Mã voucher <input name="code" placeholder="VD: SALE50K" required></label>
            <button class="secondary">Áp dụng</button>
          </form>
        </c:when>
        <c:otherwise><p>Đã áp voucher ✔</p></c:otherwise>
      </c:choose>

      <%-- 4. Phương thức thanh toán --%>
      <form method="post" action="${ctx}/order/detail">
        <input type="hidden" name="id" value="${o.id}">
        <input type="hidden" name="action" value="payment">
        <label><input type="radio" name="method" value="cod" ${o.paymentMethod == 'COD' ? 'checked' : ''}> Thanh toán khi nhận hàng (COD)</label>
        <label><input type="radio" name="method" value="online" ${o.paymentMethod == 'ONLINE' ? 'checked' : ''}> Thanh toán online</label>
        <button>Xác nhận phương thức</button>
      </form>

      <%-- Chọn online xong → đơn "chờ thanh toán" → sang trang thanh toán của F5 --%>
      <c:if test="${o.status == 'AWAITING_PAYMENT'}">
        <p><a class="btn-link" href="${ctx}/payment/checkout?orderId=${o.id}">Thanh toán ngay →</a></p>
      </c:if>
    </section>
  </c:if>

  <%-- ========== CÁC NÚT THEO TRẠNG THÁI ========== --%>
  <section class="order-card actions">
    <%-- Theo dõi vận chuyển (F6) khi đơn đã đi giao --%>
    <c:if test="${o.status == 'SHIPPING' or o.status == 'DELIVERED' or o.status == 'COMPLETED'}">
      <a href="${ctx}/shipping/track?orderId=${o.id}">Theo dõi vận chuyển</a>
    </c:if>

    <%-- Người mua xác nhận đã nhận hàng → đơn hoàn tất --%>
    <c:if test="${isBuyer and (o.status == 'SHIPPING' or o.status == 'DELIVERED')}">
      <form method="post" action="${ctx}/order/detail">
        <input type="hidden" name="id" value="${o.id}">
        <input type="hidden" name="action" value="received">
        <button>Đã nhận hàng</button>
      </form>
    </c:if>

    <%-- F8: đơn hoàn tất → người mua và người bán đều đánh giá được bên kia --%>
    <c:if test="${o.status == 'COMPLETED'}">
       <a href="${ctx}/review/create?orderId=${o.id}">Đánh giá giao dịch</a>
    </c:if>

    <%-- Huỷ đơn: khi đơn chưa đi giao (luật canMoveTo(CANCELLED) trong Order) --%>
    <c:if test="${editable or o.status == 'PAID'}">
      <form method="post" action="${ctx}/order/detail">
        <input type="hidden" name="id" value="${o.id}">
        <input type="hidden" name="action" value="cancel">
        <input name="reason" placeholder="Lý do huỷ" required>
        <button class="secondary">Huỷ đơn</button>
      </form>
    </c:if>
  </section>
</div>
<%@ include file="/WEB-INF/views/common/footer.jsp" %>