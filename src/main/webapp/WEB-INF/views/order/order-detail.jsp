<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN" scope="request"/>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%-- Chi tiết + thanh toán 1 đơn. Dữ liệu từ OrderServlet.showDetail:
     order, isBuyer, isSeller, editable, codConfirmed, shipment, carrierName, payments, dispute, refund, payout, error, message
     + khi người mua còn sửa được đơn: addresses, quotes, vouchers, codProblem --%>
<c:set var="pageTitle" value="Đơn ${order.code}"/>
<c:set var="pageCss" value="order.css"/>
<c:set var="backUrl" value="/order/list"/>
<%@ include file="/WEB-INF/views/common/header.jsp" %>
<c:set var="o" value="${order}"/>
<c:set var="disputeOpen" value="${not empty dispute and dispute.open}"/>
<div class="order-page">
  <h1>Đơn <c:out value="${o.code}"/> <span class="status status-${o.status}">${o.status.label()}</span></h1>
  <p class="hint">
    Ngày đặt: ${o.orderDate.toLocalDate()} ·
    Bạn là <strong>${isBuyer ? 'người mua' : 'người bán'}</strong> ·
    <a href="${ctx}/order/list?as=${isBuyer ? 'buyer' : 'seller'}">Danh sách đơn</a>
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
          <td><a href="${ctx}/product?id=${item.productId}"><c:out value="${item.productId}"/></a></td>
          <td>${item.quantity}</td>
          <td><fmt:formatNumber value="${item.unitPrice}" pattern="#,##0"/> đ</td>
        </tr>
      </c:forEach>
    </table>
    <table class="money">
      <tr><td>Tiền hàng</td><td><fmt:formatNumber value="${o.subTotal}" pattern="#,##0"/> đ</td></tr>
      <tr><td>Phí vận chuyển</td><td>
        <c:choose>
          <c:when test="${empty shipment}">chưa chọn</c:when>
          <c:otherwise><fmt:formatNumber value="${o.shippingFee}" pattern="#,##0"/> đ</c:otherwise>
        </c:choose></td></tr>
      <tr><td>Giảm giá</td><td>− <fmt:formatNumber value="${o.discountAmount}" pattern="#,##0"/> đ</td></tr>
      <tr class="total"><td>Tổng thanh toán</td><td><fmt:formatNumber value="${o.totalAmount}" pattern="#,##0"/> đ</td></tr>
    </table>
  </section>

  <%-- ========== ĐẶT HÀNG & THANH TOÁN: người mua, khi đơn còn sửa được ========== --%>
  <c:if test="${isBuyer and editable}">
    <section class="order-card checkout-steps">
      <h2>Đặt hàng &amp; thanh toán</h2>

      <%-- 1. Địa chỉ giao: chọn từ sổ địa chỉ (F1). Đổi địa chỉ thì phí ship tính lại. --%>
      <h3>1. Địa chỉ giao</h3>
      <form method="post" action="${ctx}/order/detail">
        <input type="hidden" name="id" value="${o.id}">
        <input type="hidden" name="action" value="address">
        <select name="addressId">
          <c:forEach var="a" items="${addresses}">
            <option value="${a.id}" ${a.id == o.shippingAddressId ? 'selected' : ''}>
              <c:out value="${a.label}: ${a.addressLine}${empty a.ward ? '' : ', '.concat(a.ward)}${empty a.district ? '' : ', '.concat(a.district)}, ${a.province}"/>
            </option>
          </c:forEach>
        </select>
        <button class="secondary">Lưu địa chỉ</button>
      </form>

      <%-- 2. Đơn vị vận chuyển (F6): phí đã tính theo tỉnh người bán → tỉnh giao --%>
      <h3>2. Đơn vị vận chuyển</h3>
      <form method="post" action="${ctx}/order/detail">
        <input type="hidden" name="id" value="${o.id}">
        <input type="hidden" name="action" value="carrier">
        <div class="carrier-list">
          <c:forEach var="q" items="${quotes}" varStatus="st">
            <label class="choice">
              <input type="radio" name="carrierId" value="${fn:escapeXml(q.carrier().id)}" required
                     ${q.carrier().id == shipment.carrierId ? 'checked' : ''}>
              <strong><c:out value="${q.carrier().name}"/></strong>
              · <fmt:formatNumber value="${q.fee()}" pattern="#,##0"/> đ
              · khoảng ${q.carrier().estimatedDays} ngày
            </label>
          </c:forEach>
        </div>
        <button class="secondary">${empty shipment ? 'Chọn đơn vị này' : 'Đổi đơn vị vận chuyển'}</button>
      </form>

      <%-- 3. Voucher (F4): gợi ý voucher còn dùng được của người mua --%>
      <h3>3. Voucher</h3>
      <c:choose>
        <c:when test="${empty o.voucherId}">
          <form method="post" action="${ctx}/order/detail">
            <input type="hidden" name="id" value="${o.id}">
            <input type="hidden" name="action" value="voucher">
            <input name="code" list="my-vouchers" placeholder="Nhập mã, VD: GIAM50K" required>
            <datalist id="my-vouchers">
              <c:forEach var="v" items="${vouchers}">
                <option value="${fn:escapeXml(v.code)}">Giảm <fmt:formatNumber value="${v.discountValue}" pattern="#,##0"/> đ, đơn từ <fmt:formatNumber value="${v.minOrderValue}" pattern="#,##0"/> đ</option>
              </c:forEach>
            </datalist>
            <button class="secondary">Áp dụng</button>
          </form>
          <c:if test="${not empty vouchers}">
            <p class="hint">Voucher của bạn:
              <c:forEach var="v" items="${vouchers}" varStatus="st">
                <strong><c:out value="${v.code}"/></strong> (giảm <fmt:formatNumber value="${v.discountValue}" pattern="#,##0"/> đ, đơn từ <fmt:formatNumber value="${v.minOrderValue}" pattern="#,##0"/> đ<c:if test="${not empty v.expiryDate}">, hạn ${v.expiryDate}</c:if>)${st.last ? '' : ' · '}
              </c:forEach>
            </p>
          </c:if>
        </c:when>
        <c:otherwise><p>Đã áp voucher ✔</p></c:otherwise>
      </c:choose>

      <%-- 4. Thanh toán (F5): phải chọn vận chuyển trước để tổng tiền đã gồm phí ship --%>
      <h3>4. Thanh toán</h3>
      <c:choose>
        <c:when test="${empty shipment}">
          <p class="hint">Chọn đơn vị vận chuyển trước để biết tổng tiền.</p>
        </c:when>
        <c:otherwise>
          <div class="actions">
            <c:choose>
              <c:when test="${empty codProblem}">
                <form method="post" action="${ctx}/order/detail">
                  <input type="hidden" name="id" value="${o.id}">
                  <input type="hidden" name="action" value="payment">
                  <input type="hidden" name="method" value="cod">
                  <button>Đặt hàng, trả tiền khi nhận (COD)</button>
                </form>
              </c:when>
              <c:otherwise><p class="tag-off"><c:out value="${codProblem}"/></p></c:otherwise>
            </c:choose>
            <form method="post" action="${ctx}/order/detail">
              <input type="hidden" name="id" value="${o.id}">
              <input type="hidden" name="action" value="payment">
              <input type="hidden" name="method" value="online">
              <button>${o.status == 'AWAITING_PAYMENT' ? 'Tiếp tục thanh toán online →' : 'Thanh toán online →'}</button>
            </form>
          </div>
        </c:otherwise>
      </c:choose>
    </section>
  </c:if>

  <%-- ========== VẬN CHUYỂN ========== --%>
  <c:if test="${not empty shipment}">
    <section class="order-card">
      <h2>Vận chuyển</h2>
      <p><strong><c:out value="${carrierName}"/></strong> · ${shipment.status.label()}
        <c:if test="${not empty shipment.trackingCode}"> · mã vận đơn <c:out value="${shipment.trackingCode}"/></c:if>
        · <a href="${ctx}/shipping/track?orderId=${o.id}">Theo dõi vận chuyển</a></p>
      <c:if test="${codConfirmed}">
        <p>Thu hộ (COD): <fmt:formatNumber value="${shipment.codAmount}" pattern="#,##0"/> đ
          <c:if test="${isBuyer and o.status == 'NEW'}"> · Bạn đã đặt hàng, trả tiền cho shipper khi nhận hàng.</c:if></p>
      </c:if>

      <%-- Người bán gửi hàng khi người mua đã trả online hoặc đã xác nhận COD --%>
      <c:if test="${isSeller and shipment.status == 'CREATED'}">
        <c:choose>
          <c:when test="${o.status == 'PAID' or (o.status == 'NEW' and codConfirmed)}">
            <form method="post" action="${ctx}/shipping/track">
              <input type="hidden" name="orderId" value="${o.id}">
              <input type="hidden" name="action" value="ship">
              <input name="trackingCode" required placeholder="Mã vận đơn">
              <button>Đã gửi hàng</button>
            </form>
          </c:when>
          <c:when test="${o.status == 'NEW' or o.status == 'AWAITING_PAYMENT'}">
            <p class="hint">Chờ người mua thanh toán hoặc xác nhận COD rồi mới gửi hàng.</p>
          </c:when>
        </c:choose>
      </c:if>
    </section>
  </c:if>
  <c:if test="${isSeller and empty shipment and (o.status == 'NEW' or o.status == 'AWAITING_PAYMENT')}">
    <p class="hint">Người mua chưa chọn đơn vị vận chuyển và thanh toán.</p>
  </c:if>

  <%-- ========== LỊCH SỬ THANH TOÁN (F5) ========== --%>
  <c:if test="${not empty payments}">
    <section class="order-card">
      <h2>Lịch sử thanh toán</h2>
      <table class="order-table">
        <tr><th>Mã</th><th>Cổng</th><th>Số tiền</th><th>Trạng thái</th><th>Thời gian</th></tr>
        <c:forEach var="p" items="${payments}">
          <tr>
            <td><c:out value="${p.code}"/></td>
            <td>${p.gateway.displayName}</td>
            <td><fmt:formatNumber value="${p.amount}" pattern="#,##0"/> đ</td>
            <td>${p.status.displayName}</td>
            <td>${empty p.paidAt ? p.createdAt.toLocalDate() : p.paidAt.toLocalDate()}</td>
          </tr>
        </c:forEach>
      </table>
    </section>
  </c:if>

  <%-- ========== HOÀN TIỀN / TIỀN NGƯỜI BÁN NHẬN (F9) ========== --%>
  <c:if test="${not empty refund}">
    <p class="order-alert">Đã hoàn <strong><fmt:formatNumber value="${refund.amount}" pattern="#,##0"/> đ</strong>
      cho người mua (${refund.reason.displayName}).</p>
  </c:if>
  <c:if test="${not empty payout}">
    <p class="order-alert">Tiền bán hàng: <fmt:formatNumber value="${payout.grossAmount}" pattern="#,##0"/> đ
      − phí sàn <fmt:formatNumber value="${payout.commissionFee}" pattern="#,##0"/> đ
      = <strong><fmt:formatNumber value="${payout.netAmount}" pattern="#,##0"/> đ</strong>
      · ${payout.status.displayName} · <a href="${ctx}/seller/payouts">Thu nhập của tôi</a></p>
  </c:if>
  <c:if test="${not empty dispute}">
    <p class="order-alert ${disputeOpen ? 'error' : ''}">Khiếu nại: ${dispute.status.displayName} ·
      <a href="${ctx}/dispute?id=${dispute.id}">Xem khiếu nại</a></p>
  </c:if>

  <%-- ========== CÁC NÚT THEO TRẠNG THÁI ========== --%>
  <section class="order-card actions">
    <%-- Người mua xác nhận đã nhận hàng → đơn hoàn tất, tiền chuyển cho người bán --%>
    <c:if test="${isBuyer and not disputeOpen and (o.status == 'SHIPPING' or o.status == 'DELIVERED')}">
      <form method="post" action="${ctx}/order/detail">
        <input type="hidden" name="id" value="${o.id}">
        <input type="hidden" name="action" value="received">
        <button>Đã nhận hàng</button>
      </form>
      <a class="btn-link secondary" href="${ctx}/dispute?orderId=${o.id}">Khiếu nại</a>
    </c:if>
    <c:if test="${isBuyer and o.status == 'DELIVERED' and not disputeOpen}">
      <span class="hint">Đơn tự hoàn tất sau 7 ngày kể từ khi giao nếu bạn không khiếu nại.</span>
    </c:if>

    <%-- F8: đơn hoàn tất → người mua và người bán đều đánh giá được bên kia --%>
    <c:if test="${o.status == 'COMPLETED'}">
       <a class="btn-link" href="${ctx}/review/create?orderId=${o.id}">Đánh giá giao dịch</a>
    </c:if>

    <%-- Huỷ đơn: khi hàng chưa gửi (luật canMoveTo(CANCELLED) trong Order). Đơn đã trả online → tự hoàn tiền. --%>
    <c:if test="${(o.status == 'NEW' or o.status == 'AWAITING_PAYMENT' or o.status == 'PAID') and (empty shipment or shipment.status == 'CREATED')}">
      <form method="post" action="${ctx}/order/detail">
        <input type="hidden" name="id" value="${o.id}">
        <input type="hidden" name="action" value="cancel">
        <input name="reason" placeholder="Lý do huỷ" required>
        <button class="secondary">Huỷ đơn${o.status == 'PAID' ? ' và hoàn tiền' : ''}</button>
      </form>
    </c:if>
  </section>
</div>
<%@ include file="/WEB-INF/views/common/footer.jsp" %>
