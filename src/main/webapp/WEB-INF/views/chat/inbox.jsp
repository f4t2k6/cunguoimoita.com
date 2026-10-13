<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Tin nhắn"/>
<c:set var="pageCss" value="chat.css"/>
<c:set var="pageMainClass" value="page-wide"/>
<%@ include file="/WEB-INF/views/common/header.jsp" %>
<section class="msgr msgr-inbox">
  <%@ include file="/WEB-INF/views/chat/_danh-sach.jsp" %>
  <div class="msgr-chat msgr-placeholder">
    <svg viewBox="0 0 48 48" fill="none" stroke="#000" stroke-width="1.6" aria-hidden="true">
      <path d="M24 6C13.5 6 6 13.3 6 22.5c0 5 2.3 9.4 6 12.4V42l6.6-3.6c1.7.5 3.5.7 5.4.7 10.5 0 18-7.3 18-16.6S34.5 6 24 6z"/>
    </svg>
    <p>Chọn một đoạn chat để xem tin nhắn</p>
  </div>
</section>
<%@ include file="/WEB-INF/views/common/footer.jsp" %>
