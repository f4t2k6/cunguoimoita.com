<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN" scope="request"/>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%-- Chi tiết 1 đơn, khác nhau theo vai trò. Dữ liệu từ OrderServlet.showDetail:
     order, itemCards (productId → tên + ảnh), isBuyer, isSeller, editable, codConfirmed, shipment, carrierName,
     dispute, refund, payout, sellerAddresses (người bán), error, message
     + khi người mua còn sửa được đơn: addresses, quotes, vouchers, codProblem
     Người mua: đang tạo đơn → các bước đặt hàng (không có Huỷ đơn); đã đặt → trạng thái, vận chuyển, huỷ đơn.
     Người bán: thông tin đơn, xác nhận giao hàng cho đơn vị vận chuyển (bưu cục / nhân viên đến lấy), huỷ đơn. --%>
<c:set var="pageTitle" value="Đơn ${order.code}"/>
<c:set var="pageCss" value="order.css"/>
<c:set var="backUrl" value="/order/list"/>
<%@ include file="/WEB-INF/views/common/header.jsp" %>
<c:set var="o" value="${order}"/>
<c:set var="disputeOpen" value="${not empty dispute and dispute.open}"/>
<c:set var="closed" value="${o.status == 'CANCELLED' or o.status == 'REFUNDED'}"/>
<%-- Người mua đã trả tiền (online) hoặc đã đặt COD → vận đơn đã có mã, chờ người bán giao --%>
<c:set var="placed" value="${not closed and (o.status != 'NEW' and o.status != 'AWAITING_PAYMENT' or codConfirmed)}"/>
<%-- Bước trên thanh trạng thái: 1 chờ người bán xác nhận, 2 chuẩn bị vận chuyển, 3 đang vận chuyển, 4 đã giao --%>
<c:set var="step" value="${empty shipment ? 1 : (shipment.status == 'RETURNED' ? 0 : shipment.status.ordinal() + 1)}"/>
<c:set var="canCancel" value="${not closed and (o.status == 'NEW' or o.status == 'AWAITING_PAYMENT' or o.status == 'PAID') and (empty shipment or shipment.status == 'CREATED') and (isSeller or not editable)}"/>
<div class="order-page page-card">
  <div class="page-card-head">
    <p class="page-eyebrow">${isBuyer ? 'ĐƠN MUA' : 'ĐƠN BÁN'}</p>
    <h1 class="page-title">Đơn <c:out value="${o.code}"/> <span class="status status-${o.status}">${o.status.label()}</span></h1>
  </div>
  <p class="hint">
    Ngày đặt: ${o.orderDate.toLocalDate()} ·
    Bạn là <strong>${isBuyer ? 'người mua' : 'người bán'}</strong> ·
    <a href="${ctx}/order/list?as=${isBuyer ? 'buyer' : 'seller'}">${isBuyer ? 'Đơn mua' : 'Đơn bán'}</a>
    <c:if test="${isBuyer}"> · <a href="${ctx}/payment/history">Lịch sử thanh toán</a></c:if>
  </p>

  <c:if test="${not empty error}"><p class="order-alert error"><c:out value="${error}"/></p></c:if>
  <c:if test="${not empty message}"><p class="order-alert"><c:out value="${message}"/></p></c:if>

  <%-- TRẠNG THÁI ĐƠN --%>
  <c:choose>
    <c:when test="${o.status == 'CANCELLED'}"><p class="order-state-banner">Đơn đã huỷ.</p></c:when>
    <c:when test="${o.status == 'REFUNDED'}"><p class="order-state-banner">Đơn đã hoàn tiền cho người mua.</p></c:when>
    <c:when test="${placed}">
      <ol class="order-steps" aria-label="Trạng thái đơn">
        <li class="${step >= 1 ? 'done' : ''} ${step == 1 ? 'current' : ''}"><span>1</span>Chờ người bán xác nhận</li>
        <li class="${step >= 2 ? 'done' : ''} ${step == 2 ? 'current' : ''}"><span>2</span>Chuẩn bị vận chuyển</li>
        <li class="${step >= 3 ? 'done' : ''} ${step == 3 ? 'current' : ''}"><span>3</span>Đang vận chuyển</li>
        <li class="${step >= 4 ? 'done' : ''} ${step == 4 ? 'current' : ''}"><span>4</span>Đã giao thành công</li>
      </ol>
      <%-- Người mua: thay cho ô Vận chuyển (đã ẩn), chỉ hiện hãng + mã vận đơn --%>
      <c:if test="${isBuyer and not empty shipment and not empty shipment.trackingCode}">
        <p class="order-tracking"><c:out value="${carrierName}"/> · mã vận đơn <strong><c:out value="${shipment.trackingCode}"/></strong></p>
      </c:if>
      <c:if test="${shipment.status == 'RETURNED'}"><p class="order-state-banner">Đơn vị vận chuyển đã hoàn hàng về người bán.</p></c:if>
      <c:if test="${o.status == 'COMPLETED'}"><p class="hint">Đơn đã hoàn tất.</p></c:if>
    </c:when>
    <c:when test="${isSeller}"><p class="order-state-banner">Chờ người mua chọn vận chuyển và thanh toán.</p></c:when>
  </c:choose>

  <%-- CÁC MÓN + TIỀN --%>
  <section class="order-card">
    <h2>Sản phẩm</h2>
    <div class="table-scroll">
      <table class="order-table order-items-table">
        <tr><th>Sản phẩm</th><th>Mã sản phẩm</th><th>Số lượng</th><th>Đơn giá</th><th>Thành tiền</th></tr>
        <c:forEach var="item" items="${o.items}">
          <c:set var="card" value="${itemCards[item.productId]}"/>
          <tr>
            <td>
              <a class="order-item" href="${ctx}/product?id=${fn:escapeXml(item.productId)}">
                <c:choose>
                  <c:when test="${not empty card.coverImage()}"><img class="cart-thumb" src="${ctx}<c:out value='${card.coverImage()}'/>" alt="" loading="lazy"></c:when>
                  <c:otherwise><span class="cart-thumb cart-thumb-empty">Chưa có ảnh</span></c:otherwise>
                </c:choose>
                <span class="order-item-name"><c:out value="${empty card ? item.productId : card.name()}"/></span>
              </a>
            </td>
            <td class="order-item-code"><c:out value="${item.productId}"/></td>
            <td>${item.quantity}</td>
            <td><fmt:formatNumber value="${item.unitPrice}" pattern="#,##0"/> đ</td>
            <td><strong><fmt:formatNumber value="${item.unitPrice * item.quantity}" pattern="#,##0"/> đ</strong></td>
          </tr>
        </c:forEach>
      </table>
    </div>
    <table class="money">
      <tr><td>Tiền hàng</td><td><fmt:formatNumber value="${o.subTotal}" pattern="#,##0"/> đ</td></tr>
      <tr><td>Phí vận chuyển</td><td>
        <c:choose>
          <c:when test="${empty shipment}">chưa chọn</c:when>
          <c:otherwise><fmt:formatNumber value="${o.shippingFee}" pattern="#,##0"/> đ</c:otherwise>
        </c:choose></td></tr>
      <tr><td>Giảm giá</td><td>− <fmt:formatNumber value="${o.discountAmount}" pattern="#,##0"/> đ</td></tr>
      <tr><td>Phí dịch vụ</td><td>0 đ</td></tr>
      <tr class="total"><td>Tổng thanh toán</td><td><fmt:formatNumber value="${o.totalAmount}" pattern="#,##0"/> đ</td></tr>
      <%-- Người bán: phí sàn trừ vào tiền nhận (khi đơn đã hoàn tất và có khoản thu nhập) --%>
      <c:if test="${isSeller and not empty payout}">
        <tr><td>Phí sàn (trừ vào tiền bạn nhận)</td><td>− <fmt:formatNumber value="${payout.commissionFee}" pattern="#,##0"/> đ</td></tr>
        <tr><td>Bạn thực nhận</td><td><strong><fmt:formatNumber value="${payout.netAmount}" pattern="#,##0"/> đ</strong></td></tr>
      </c:if>
    </table>
  </section>

  <%-- ĐẶT HÀNG & THANH TOÁN: người mua, khi đơn còn sửa được --%>
  <c:if test="${isBuyer and editable}">
    <section class="order-card checkout-steps" id="dat-hang">
      <h2>Đặt hàng &amp; thanh toán</h2>

      <%-- 1. Địa chỉ giao: chọn từ sổ địa chỉ. Chọn xong là tự lưu (không cần nút), phí ship tính lại. --%>
      <h3>1. Địa chỉ giao</h3>
      <%-- #dat-hang: tải lại xong trình duyệt cuộn về đúng khung này, không nhảy lên đầu trang --%>
      <form method="post" action="${ctx}/order/detail#dat-hang">
        <input type="hidden" name="id" value="${o.id}">
        <input type="hidden" name="action" value="address">
        <select name="addressId" onchange="this.form.submit()" aria-label="Địa chỉ giao (chọn là tự lưu)">
          <c:forEach var="a" items="${addresses}">
            <option value="${a.id}" ${a.id == o.shippingAddressId ? 'selected' : ''}>
              <c:out value="${a.label}: ${a.addressLine}${empty a.ward ? '' : ', '.concat(a.ward)}${empty a.district ? '' : ', '.concat(a.district)}, ${a.province}"/>
            </option>
          </c:forEach>
        </select>
        <noscript><button class="secondary">Lưu địa chỉ</button></noscript>
      </form>

      <%-- 2. Đơn vị vận chuyển: phí đã tính theo tỉnh người bán → tỉnh giao. Bấm chọn là tự lưu (không cần nút). --%>
      <h3>2. Đơn vị vận chuyển</h3>
      <form method="post" action="${ctx}/order/detail#dat-hang">
        <input type="hidden" name="id" value="${o.id}">
        <input type="hidden" name="action" value="carrier">
        <div class="carrier-list">
          <c:forEach var="q" items="${quotes}" varStatus="st">
            <label class="choice">
              <input type="radio" name="carrierId" value="${fn:escapeXml(q.carrier().id)}" required onchange="this.form.submit()"
                     ${q.carrier().id == shipment.carrierId ? 'checked' : ''}>
              <strong><c:out value="${q.carrier().name}"/></strong>
              · <fmt:formatNumber value="${q.fee()}" pattern="#,##0"/> đ
              · khoảng ${q.carrier().estimatedDays} ngày
            </label>
          </c:forEach>
        </div>
        <noscript><button class="secondary">${empty shipment ? 'Chọn đơn vị này' : 'Đổi đơn vị vận chuyển'}</button></noscript>
      </form>

      <%-- 3. Voucher: gợi ý voucher còn dùng được của người mua --%>
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

      <%-- 4. Thanh toán: phải chọn vận chuyển trước để tổng tiền đã gồm phí ship --%>
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


  <%-- ========== VẬN CHUYỂN (sau khi đã đặt: mã vận đơn tự sinh) ==========
       Chỉ người bán thấy, sau khi đã xác nhận giao hàng (trước đó thông tin nằm trong ô Xác nhận giao hàng bên dưới).
       Người mua không thấy ô này: trạng thái đã có ở thanh 4 bước phía trên. --%>
  <c:if test="${isSeller and placed and not empty shipment and shipment.status != 'CREATED'}">
    <section class="order-card">
      <h2>Vận chuyển</h2>
      <p><strong><c:out value="${carrierName}"/></strong> · ${shipment.status.label()}
        <c:if test="${not empty shipment.trackingCode}"> · mã vận đơn <strong><c:out value="${shipment.trackingCode}"/></strong></c:if></p>
      <c:if test="${codConfirmed}">
        <p>Thu hộ (COD): <fmt:formatNumber value="${shipment.codAmount}" pattern="#,##0"/> đ</p>
      </c:if>
      <a class="btn-link secondary" href="${ctx}/shipping/track?orderId=${o.id}">Xem vận chuyển →</a>
    </section>
  </c:if>

  <%-- NGƯỜI BÁN: XÁC NHẬN GIAO HÀNG CHO ĐƠN VỊ VẬN CHUYỂN --%>
  <c:if test="${isSeller and placed and not empty shipment and shipment.status == 'CREATED'}">
    <section class="order-card" id="ban-giao">
      <h2>Xác nhận giao hàng</h2>
      <p><strong><c:out value="${carrierName}"/></strong>
        <c:if test="${not empty shipment.trackingCode}"> · mã vận đơn <strong><c:out value="${shipment.trackingCode}"/></strong></c:if></p>
      <c:if test="${codConfirmed}">
        <p>Thu hộ (COD): <strong><fmt:formatNumber value="${shipment.codAmount}" pattern="#,##0"/> đ</strong></p>
      </c:if>
      <p class="hint">Mã vận đơn đã được tạo tự động. Đóng gói xong, chọn cách giao hàng cho
        <strong><c:out value="${carrierName}"/></strong>:</p>
      <form method="post" action="${ctx}/shipping/track" class="handover-form">
        <input type="hidden" name="orderId" value="${o.id}">
        <input type="hidden" name="action" value="handover">
        <label class="handover-option">
          <input type="radio" name="handover" value="dropoff" required checked>
          <span><strong>Mang đến bưu cục gần nhất</strong><small>Bạn tự mang gói hàng ra bưu cục của <c:out value="${carrierName}"/>.</small></span>
        </label>
        <label class="handover-option">
          <input type="radio" name="handover" value="pickup" ${empty sellerAddresses ? 'disabled' : ''}>
          <span><strong>Nhân viên đến lấy hàng</strong><small>Chọn địa chỉ trong sổ địa chỉ của bạn:</small>
            <c:choose>
              <c:when test="${empty sellerAddresses}">
                <small class="tag-off">Bạn chưa có địa chỉ nào. <a href="${ctx}/account/profile#so-dia-chi">Thêm địa chỉ</a></small>
              </c:when>
              <c:otherwise>
                <select name="addressId">
                  <c:forEach var="a" items="${sellerAddresses}">
                    <option value="${a.id}"><c:out value="${a.label}: ${a.addressLine}${empty a.ward ? '' : ', '.concat(a.ward)}${empty a.district ? '' : ', '.concat(a.district)}, ${a.province}"/></option>
                  </c:forEach>
                </select>
              </c:otherwise>
            </c:choose>
          </span>
        </label>
        <button>Xác nhận giao hàng</button>
      </form>
    </section>
  </c:if>

  <%-- HOÀN TIỀN / TIỀN NGƯỜI BÁN NHẬN / KHIẾU NẠI --%>
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

  <%-- NGƯỜI MUA: NHẬN HÀNG / TRẢ HÀNG / ĐÁNH GIÁ --%>
  <%-- Chỉ khi vận đơn đã qua đủ các bước tới "đã giao" mới hiện 2 lựa chọn; người bán không có các nút này --%>
  <c:if test="${isBuyer and not disputeOpen and o.status == 'DELIVERED'}">
    <section class="order-card actions">
      <form method="post" action="${ctx}/order/detail">
        <input type="hidden" name="id" value="${o.id}">
        <input type="hidden" name="action" value="received">
        <button>Đã nhận hàng</button>
      </form>
      <a class="btn-link secondary" href="${ctx}/dispute?orderId=${o.id}">Trả hàng/Hoàn tiền</a>
      <p class="hint">Đơn tự hoàn tất sau 7 ngày kể từ khi giao nếu bạn không yêu cầu trả hàng/hoàn tiền.</p>
    </section>
  </c:if>
  <%-- Đơn hoàn tất → chỉ người mua đánh giá giao dịch --%>
  <c:if test="${isBuyer and o.status == 'COMPLETED'}">
    <c:choose>
      <c:when test="${not empty myReview}">
        <section class="order-card my-review">
          <h2>Đánh giá của bạn</h2>
          <p class="review-stars" aria-label="${myReview.rating} trên 5 sao">
            <c:forEach var="i" begin="1" end="5"><span class="${i <= myReview.rating ? 'on' : ''}">★</span></c:forEach>
            <small>${myReview.rating}/5</small>
          </p>
          <c:choose>
            <c:when test="${not empty myReview.content}"><p class="review-text pre-line"><c:out value="${myReview.content}"/></p></c:when>
            <c:otherwise><p class="hint">Bạn không để lại nhận xét.</p></c:otherwise>
          </c:choose>
          <a class="btn-link secondary" href="${ctx}/review/create?orderId=${o.id}">Chỉnh sửa đánh giá</a>
        </section>
      </c:when>
      <c:otherwise>
        <section class="order-card actions">
          <a class="btn-link" href="${ctx}/review/create?orderId=${o.id}">Đánh giá giao dịch</a>
        </section>
      </c:otherwise>
    </c:choose>
  </c:if>

  <%-- HUỶ ĐƠN: khi hàng chưa giao cho đơn vị vận chuyển. Người mua đang tạo đơn thì không có ô này. --%>
  <c:if test="${canCancel}">
    <section class="order-card cancel-box">
      <h2>Huỷ đơn hàng</h2>
      <form method="post" action="${ctx}/order/detail" class="cancel-form">
        <input type="hidden" name="id" value="${o.id}">
        <input type="hidden" name="action" value="cancel">
        <input name="reason" placeholder="Lý do huỷ" required>
        <button class="secondary">Huỷ đơn${o.status == 'PAID' ? ' và hoàn tiền' : ''}</button>
      </form>
    </section>
  </c:if>
</div>
<%@ include file="/WEB-INF/views/common/footer.jsp" %>
