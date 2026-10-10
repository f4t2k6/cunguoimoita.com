<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Thông báo"/>
<c:set var="pageCss" value="chat.css"/>
<%@ include file="/WEB-INF/views/common/header.jsp" %>
<main class="chat-page">
  <h1>Thông báo</h1>
  <form method="post" action="${pageContext.request.contextPath}/notifications/read-all">
    <button type="submit" class="secondary">Đánh dấu đã đọc tất cả</button>
  </form>
  <c:if test="${empty notifications}"><p class="chat-empty">Chưa có thông báo nào.</p></c:if>
  <ul class="inbox-list">
    <c:forEach var="n" items="${notifications}">
      <li class="${n.read ? '' : 'unread'}">
        <a href="${pageContext.request.contextPath}/notifications/open?id=${n.id}">
          <span class="inbox-who"><c:out value="${n.content}"/></span>
          <span class="inbox-last">${n.createdDate.toLocalDate()} ${n.createdDate.toLocalTime().withNano(0)}</span>
        </a>
      </li>
    </c:forEach>
  </ul>
</section>
<%@ include file="/WEB-INF/views/common/footer.jsp" %>
