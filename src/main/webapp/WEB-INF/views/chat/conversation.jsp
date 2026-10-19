<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="Tin nhắn"/>
<c:set var="pageCss" value="chat.css"/>
<c:set var="pageMainClass" value="page-wide"/>
<c:set var="hideChatDock" value="true"/>
<c:set var="backUrl" value="/chat/inbox"/>
<%@ include file="/WEB-INF/views/common/header.jsp" %>
<fmt:setLocale value="vi_VN"/>
<section class="msgr msgr-open page-card page-card-flush">
  <%@ include file="/WEB-INF/views/chat/_danh-sach.jsp" %>

  <div class="msgr-chat" data-chat-root>
    <header class="msgr-head">
      <a class="msgr-back" href="${ctx}/chat/inbox" aria-label="Quay lại danh sách">
        <svg viewBox="0 0 24 24" fill="none" stroke="#000" stroke-width="2"><path d="M15 5l-7 7 7 7"/></svg>
      </a>
      <span class="msgr-avatar"><c:choose><c:when test="${not empty conversation.otherAvatar(userId)}"><img src="${ctx}${fn:escapeXml(conversation.otherAvatar(userId))}" alt=""></c:when><c:otherwise><c:out value="${conversation.otherInitial(userId)}"/></c:otherwise></c:choose></span>
      <span class="msgr-head-text">
        <strong><c:out value="${conversation.otherName(userId)}"/></strong>
        <span>
          ${conversation.askerId == userId ? 'Người bán' : 'Người mua'} ·
          <c:out value="${empty conversation.productName ? conversation.productId : conversation.productName}"/>
          <c:if test="${conversation.productPrice != null}">
            · <fmt:formatNumber value="${conversation.productPrice}" maxFractionDigits="0"/> đ
          </c:if>
        </span>
      </span>
      <%-- F10: báo cáo bài đăng (người hỏi mua) hoặc báo cáo người hỏi (người bán) --%>
      <a class="msgr-report" href="${ctx}/report?type=${conversation.askerId == userId ? 'product' : 'user'}&amp;id=${fn:escapeXml(conversation.askerId == userId ? conversation.productId : conversation.askerId)}">Báo cáo</a>
    </header>

    <div id="chat-box" class="msgr-thread"
         data-ctx="${ctx}"
         data-conversation="${fn:escapeXml(conversation.id)}"
         data-other-initial="${fn:escapeXml(conversation.otherInitial(userId))}"
         data-other-avatar="${fn:escapeXml(conversation.otherAvatar(userId))}"></div>

    <%-- Khung món đang trao đổi, chat.js luôn đặt ở đầu đoạn chat (kể cả khi chưa có tin nào) --%>
    <template id="chat-product" data-chat-product>
      <div class="msgr-product">
        <a class="msgr-product-photo" href="${ctx}/product?id=${fn:escapeXml(conversation.productId)}">
          <c:choose>
            <c:when test="${not empty conversation.productImage}">
              <img src="${ctx}<c:out value='${conversation.productImage}'/>" alt="<c:out value='${conversation.productName}'/>">
            </c:when>
            <c:otherwise><span>Chưa có ảnh</span></c:otherwise>
          </c:choose>
        </a>
        <div class="msgr-product-info">
          <span class="msgr-product-label">${conversation.askerId == userId ? 'Bạn đang hỏi mua' : 'Người mua đang hỏi về'}</span>
          <a class="msgr-product-name" href="${ctx}/product?id=${fn:escapeXml(conversation.productId)}"><c:out value="${empty conversation.productName ? conversation.productId : conversation.productName}"/></a>
          <c:if test="${conversation.productPrice != null}">
            <strong class="msgr-product-price">Giá đăng: <fmt:formatNumber value="${conversation.productPrice}" maxFractionDigits="0"/> đ</strong>
          </c:if>
          <c:if test="${not empty conversation.productCondition}">
            <span class="msgr-product-meta">Tình trạng: ${conversation.productCondition.label}</span>
          </c:if>
          <c:if test="${conversation.open and conversation.askerId == userId}">
            <div class="msgr-product-actions">
              <button type="button" class="msgr-pill light" data-open-offer>Trả giá món này</button>
              <%-- Mua ngay với giá đăng: chỉ người mua, món còn hàng và có bán (ẩn với người bán, món đã đặt/đã bán) --%>
              <c:if test="${conversation.productBuyable}">
                <form method="post" action="${ctx}/order/checkout" class="msgr-buy-form">
                  <input type="hidden" name="buyNow" value="${fn:escapeXml(conversation.productId)}">
                  <button type="submit" class="msgr-pill">Mua ngay</button>
                </form>
              </c:if>
            </div>
          </c:if>
        </div>
      </div>
    </template>

    <p id="chat-error" class="msgr-error" hidden></p>

    <c:choose>
      <c:when test="${conversation.open}">
        <form id="offer-form" class="msgr-offer-bar" hidden>
          <span>Trả giá</span>
          <input name="amount" type="text" inputmode="numeric" autocomplete="off" data-money
                 placeholder="Số tiền muốn trả, VD 1.500.000" aria-label="Số tiền muốn trả (đồng)">
          <span class="msgr-offer-unit">đ</span>
          <button type="submit" class="msgr-pill">Gửi giá</button>
          <button type="button" class="msgr-pill light" id="offer-cancel" data-offer-cancel>Huỷ</button>
        </form>
        <form id="chat-form" class="msgr-composer">
          <button type="button" id="offer-toggle" class="msgr-icon-btn" data-offer-toggle title="Trả giá" aria-label="Trả giá">
            <svg viewBox="0 0 24 24" fill="none" stroke="#000" stroke-width="1.8" stroke-linejoin="round">
              <path d="M3 12V4h8l10 10-8 8L3 12z"/><circle cx="7.5" cy="8.5" r="1.5"/>
            </svg>
          </button>
          <label class="msgr-icon-btn" title="Gửi ảnh" aria-label="Gửi ảnh">
            <svg viewBox="0 0 24 24" fill="none" stroke="#000" stroke-width="1.8" stroke-linejoin="round">
              <rect x="3" y="5" width="18" height="14" rx="2"/><circle cx="9" cy="10" r="1.8"/><path d="M21 16l-5-5-8 8"/>
            </svg>
            <input type="file" id="chat-image" accept="image/*" hidden>
          </label>
          <input name="content" autocomplete="off" placeholder="Aa">
          <button type="submit" class="msgr-icon-btn" title="Gửi" aria-label="Gửi">
            <svg viewBox="0 0 24 24" fill="#000"><path d="M3 20.5l18-8.5L3 3.5l2.5 7.5L15 12l-9.5 1z"/></svg>
          </button>
        </form>
      </c:when>
      <c:otherwise><p class="msgr-closed">Món này đã bán, đoạn chat đã đóng.</p></c:otherwise>
    </c:choose>
  </div>
</section>
<script src="${ctx}/assets/js/chat.js?v=${assetVer}"></script>
<%@ include file="/WEB-INF/views/common/footer.jsp" %>
