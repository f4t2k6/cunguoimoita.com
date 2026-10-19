<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN" scope="request"/>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="Hạng thành viên"/>
<c:set var="pageCss" value="admin.css"/>
<c:set var="pageMainClass" value="page-wide"/>
<%@ include file="/WEB-INF/views/common/header.jsp" %>
<div class="admin-page page-card">
  <div class="page-card-head">
    <p class="page-eyebrow">QUẢN LÝ</p>
    <h1 class="page-title">Hạng thành viên</h1>
  </div>
  <%@ include file="/WEB-INF/views/admin/_menu.jsp" %>
  <p class="admin-note">Khách lên hạng khi điểm xanh (eco points) đạt ngưỡng. Hạng Bronze luôn từ 0 điểm, hạng sau phải cần nhiều điểm hơn hạng trước.</p>

  <div class="admin-cards tiers">
    <c:forEach var="t" items="${tiers}">
      <form method="post" class="admin-card admin-form">
        <h2><c:out value="${t.name}"/> <small>${t.memberCount} thành viên</small></h2>
        <input type="hidden" name="id" value="${fn:escapeXml(t.id)}">
        <label>Từ (điểm) <input name="minEcoPoints" type="number" min="0" value="${t.minEcoPoints}" required></label>
        <label>Ưu đãi (%) <input name="discountPercent" type="number" min="0" max="50" step="0.5"
               value="<fmt:formatNumber value='${t.discountPercent}' maxFractionDigits='2' groupingUsed='false'/>" required></label>
        <label class="wide">Quyền lợi <input name="benefitNote" value="${fn:escapeXml(t.benefitNote)}" maxlength="300"></label>
        <button type="submit" class="light">Lưu</button>
      </form>
    </c:forEach>
  </div>
</div>
<%@ include file="/WEB-INF/views/common/footer.jsp" %>
