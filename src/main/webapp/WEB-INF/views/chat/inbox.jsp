<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Tin nhắn"/>
<c:set var="pageCss" value="chat.css"/>
<%@ include file="/WEB-INF/views/common/header.jsp" %>
<main class="chat-page">
  <h1>Tin nhắn</h1>
  <c:if test="${empty conversations}">
    <p class="chat-empty">Chưa có cuộc trò chuyện nào. Bấm "Nhắn tin" trên một bài đăng để bắt đầu.</p>
  </c:if>
  <ul class="inbox-list">
    <c:forEach var="cv" items="${conversations}">
      <li class="${cv.unreadCount > 0 ? 'unread' : ''}">
        <a href="${pageContext.request.contextPath}/chat/conversation?id=${cv.id}">
          <%-- TODO: hiện tên người kia và tiêu đề bài đăng khi có UserService (TV1), ProductService (TV2) --%>
          <span class="inbox-who">${cv.askerId == userId ? 'Người bán' : 'Người mua'}: <c:out value="${cv.otherParty(userId)}"/></span>
          <span class="inbox-product">Bài đăng <c:out value="${cv.productId}"/><c:if test="${cv.status == 'CLOSED'}"> · đã bán</c:if></span>
          <span class="inbox-last"><c:out value="${cv.lastMessagePreview}"/></span>
          <c:if test="${cv.unreadCount > 0}"><span class="badge">${cv.unreadCount}</span></c:if>
        </a>
      </li>
    </c:forEach>
  </ul>
</section>
<%@ include file="/WEB-INF/views/common/footer.jsp" %>
