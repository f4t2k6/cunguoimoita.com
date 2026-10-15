<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="Tin nhắn"/>
<c:set var="pageCss" value="chat.css"/>
<c:set var="pageMainClass" value="page-wide"/>
<%@ include file="/WEB-INF/views/common/header.jsp" %>
<fmt:setLocale value="vi_VN"/>
<section class="msgr msgr-open">
  <%@ include file="/WEB-INF/views/chat/_danh-sach.jsp" %>

  <div class="msgr-chat">
    <header class="msgr-head">
      <a class="msgr-back" href="${ctx}/chat/inbox" aria-label="Quay lại danh sách">
        <svg viewBox="0 0 24 24" fill="none" stroke="#000" stroke-width="2"><path d="M15 5l-7 7 7 7"/></svg>
      </a>
      <span class="msgr-avatar"><c:out value="${conversation.otherInitial(userId)}"/></span>
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
         data-other-initial="${fn:escapeXml(conversation.otherInitial(userId))}"></div>

    <p id="chat-error" class="msgr-error" hidden></p>

    <c:choose>
      <c:when test="${conversation.open}">
        <form id="offer-form" class="msgr-offer-bar" hidden>
          <span>Trả giá</span>
          <input name="amount" type="number" min="1000" step="1000" placeholder="Số tiền muốn trả (đ)">
          <button type="submit" class="msgr-pill">Gửi giá</button>
          <button type="button" class="msgr-pill light" id="offer-cancel">Huỷ</button>
        </form>
        <form id="chat-form" class="msgr-composer">
          <button type="button" id="offer-toggle" class="msgr-icon-btn" title="Trả giá" aria-label="Trả giá">
            <svg viewBox="0 0 24 24" fill="none" stroke="#000" stroke-width="1.8" stroke-linejoin="round">
              <path d="M3 12V4h8l10 10-8 8L3 12z"/><circle cx="7.5" cy="8.5" r="1.5"/>
            </svg>
          </button>
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
