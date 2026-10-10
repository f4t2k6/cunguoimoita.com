<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Trò chuyện"/>
<c:set var="pageCss" value="chat.css"/>
<%@ include file="/WEB-INF/views/common/header.jsp" %>
<main class="chat-page">
  <a class="chat-back" href="${pageContext.request.contextPath}/chat/inbox">← Hộp thư</a>
  <h1>${conversation.askerId == userId ? 'Người bán' : 'Người mua'}: <c:out value="${conversation.otherParty(userId)}"/></h1>
  <p class="chat-sub">Bài đăng <c:out value="${conversation.productId}"/></p>

  <section id="chat-box" class="chat-box"
           data-ctx="${pageContext.request.contextPath}"
           data-conversation="${conversation.id}"></section>

  <p id="chat-error" class="chat-error" hidden></p>

  <c:choose>
    <c:when test="${conversation.open}">
      <form id="chat-form" class="chat-form">
        <input name="content" autocomplete="off" placeholder="Nhập tin nhắn...">
        <button type="submit">Gửi</button>
        <button type="button" id="offer-toggle" class="secondary">Trả giá</button>
      </form>
      <form id="offer-form" class="chat-form" hidden>
        <input name="amount" type="number" min="1000" step="1000" placeholder="Số tiền muốn trả (đ)">
        <button type="submit">Gửi giá</button>
      </form>
    </c:when>
    <c:otherwise><p class="chat-empty">Món này đã bán, cuộc trò chuyện đã đóng.</p></c:otherwise>
  </c:choose>
</section>
<script src="${pageContext.request.contextPath}/assets/js/chat.js"></script>
<%@ include file="/WEB-INF/views/common/footer.jsp" %>
