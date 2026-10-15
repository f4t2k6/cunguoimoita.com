<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="Báo cáo vi phạm"/>
<c:set var="pageCss" value="admin.css"/>
<%@ include file="/WEB-INF/views/common/header.jsp" %>
<div class="admin-page">
  <c:choose>
    <c:when test="${sent}">
      <section class="admin-card report-done">
        <h1>Đã gửi báo cáo</h1>
        <p>Cảm ơn bạn. Admin sẽ xem xét và báo kết quả qua mục Thông báo.</p>
        <a class="admin-btn" href="${ctx}/home">Về trang chủ</a>
      </section>
    </c:when>
    <c:when test="${empty targetName}">
      <section class="admin-card"><h1>Không tìm thấy</h1><p>Bài đăng hoặc người dùng này không còn tồn tại.</p></section>
    </c:when>
    <c:otherwise>
      <form method="post" class="admin-card admin-form report-form">
        <h1>Báo cáo ${type == 'product' ? 'bài đăng' : 'người dùng'}</h1>
        <p class="admin-meta"><c:out value="${targetName}"/></p>
        <c:if test="${not empty error}"><p class="admin-flash error" role="alert"><c:out value="${error}"/></p></c:if>
        <input type="hidden" name="type" value="${fn:escapeXml(type)}">
        <input type="hidden" name="id" value="${fn:escapeXml(id)}">
        <fieldset class="wide">
          <legend>Lý do</legend>
          <c:forEach var="r" items="${reasons}">
            <label class="radio"><input type="radio" name="reason" value="${r.dbValue()}" required
                   ${param.reason == r.dbValue() ? 'checked' : ''}> ${r.label}</label>
          </c:forEach>
        </fieldset>
        <label class="wide">Mô tả thêm (tuỳ chọn)
          <textarea name="description" rows="4" maxlength="2000"><c:out value="${param.description}"/></textarea>
        </label>
        <button type="submit">Gửi báo cáo</button>
      </form>
    </c:otherwise>
  </c:choose>
</div>
<%@ include file="/WEB-INF/views/common/footer.jsp" %>
