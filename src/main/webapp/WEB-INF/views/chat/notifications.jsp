<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Thông báo"/>
<c:set var="pageCss" value="chat.css"/>
<%@ include file="/WEB-INF/views/common/header.jsp" %>
<section class="notify-page">
  <div class="notify-panel-head">
    <h1>Thông báo</h1>
    <form method="post" action="${ctx}/notifications/read-all">
      <button type="submit" class="notify-link">Đánh dấu tất cả đã đọc</button>
    </form>
  </div>
  <c:if test="${empty notifications}"><p class="notify-empty">Chưa có thông báo nào.</p></c:if>
  <%-- Cùng cấu trúc với danh sách xổ xuống ở header; nút "..." do common.js xử lý --%>
  <ul class="notify-list">
    <c:forEach var="n" items="${notifications}">
      <li class="notify-item ${n.read ? '' : 'unread'}" data-id="${n.id}">
        <a class="notify-main" href="${ctx}/notifications/open?id=${n.id}">
          <span class="notify-icon type-${n.type.dbValue()}"></span>
          <span class="notify-text">
            <span class="notify-content"><c:out value="${n.content}"/></span>
            <time class="notify-time" data-ts="${n.createdEpochMs}"></time>
          </span>
          <span class="notify-dot"></span>
        </a>
        <button type="button" class="notify-more" aria-label="Tuỳ chọn" aria-haspopup="true">⋯</button>
        <div class="notify-menu" hidden>
          <a href="${ctx}/notifications/open?id=${n.id}">Mở thông báo</a>
          <c:if test="${!n.read}"><button type="button" class="notify-mark-read">Đánh dấu đã đọc</button></c:if>
        </div>
      </li>
    </c:forEach>
  </ul>
</section>
<%@ include file="/WEB-INF/views/common/footer.jsp" %>
