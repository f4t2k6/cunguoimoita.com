<%@ page pageEncoding="UTF-8" %>
<%-- Cột danh sách đoạn chat bên trái (kiểu Messenger). Được include tĩnh từ inbox.jsp và conversation.jsp.
     Cần: conversations, userId; conversation (đoạn đang mở, có thể trống). --%>
<aside class="msgr-list">
  <h1 class="msgr-title">Đoạn chat</h1>
  <c:if test="${empty conversations}">
    <p class="msgr-empty">Chưa có đoạn chat nào. Bấm "Nhắn tin" trên một bài đăng để bắt đầu.</p>
  </c:if>
  <ul>
    <c:forEach var="cv" items="${conversations}">
      <li>
        <a href="${ctx}/chat/conversation?id=${cv.id}"
           class="msgr-item ${cv.id == conversation.id ? 'active' : ''} ${cv.unreadCount > 0 ? 'unread' : ''}">
          <span class="msgr-avatar"><c:out value="${cv.otherInitial(userId)}"/></span>
          <span class="msgr-item-body">
            <span class="msgr-item-name"><c:out value="${cv.otherName(userId)}"/></span>
            <span class="msgr-item-product"><c:out value="${empty cv.productName ? cv.productId : cv.productName}"/><c:if test="${!cv.open}"> · đã bán</c:if></span>
            <span class="msgr-item-last">
              <span class="msgr-item-text"><c:out value="${empty cv.lastMessagePreview ? 'Bắt đầu trò chuyện' : cv.lastMessagePreview}"/></span>
              <span class="msgr-item-time">· ${cv.lastTimeLabel}</span>
            </span>
          </span>
          <c:if test="${cv.unreadCount > 0}"><span class="msgr-dot" title="${cv.unreadCount} tin chưa đọc"></span></c:if>
        </a>
      </li>
    </c:forEach>
  </ul>
</aside>
