<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN" scope="request"/>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="Đơn vị vận chuyển"/>
<c:set var="pageCss" value="admin.css"/>
<c:set var="pageMainClass" value="page-wide"/>
<%@ include file="/WEB-INF/views/common/header.jsp" %>
<div class="admin-page">
  <%@ include file="/WEB-INF/views/admin/_menu.jsp" %>
  <h1>Đơn vị vận chuyển</h1>
  <p class="admin-note">Phí giao = phí cơ bản, cộng thêm <fmt:formatNumber value="15000" pattern="#,##0"/> đ nếu người bán và người mua khác tỉnh/thành. Hãng bị tắt không hiện ở trang thanh toán.</p>

  <form method="post" class="admin-card admin-form">
    <h2>Thêm đơn vị vận chuyển</h2>
    <input type="hidden" name="action" value="save">
    <input type="hidden" name="active" value="true">
    <label>Tên <input name="name" required maxlength="100" placeholder="VD: J&amp;T Express"></label>
    <label>Hotline <input name="hotline" maxlength="20"></label>
    <label>Phí cơ bản (đ) <input name="baseFee" type="number" min="0" step="1000" required></label>
    <label>Số ngày giao <input name="estimatedDays" type="number" min="1" max="60" required></label>
    <label class="wide">Logo (URL, tuỳ chọn) <input name="logoUrl" maxlength="255"></label>
    <button type="submit">Thêm</button>
  </form>

  <div class="admin-table-wrap">
    <table class="admin-table">
      <thead><tr><th>Tên</th><th>Hotline</th><th>Phí cơ bản (đ)</th><th>Số ngày</th><th>Logo (URL)</th><th>Đang dùng</th><th></th></tr></thead>
      <tbody>
        <c:forEach var="s" items="${carriers}">
          <c:set var="fid" value="cr-${fn:escapeXml(s.id)}"/>
          <tr class="${s.active ? '' : 'is-off'}">
            <td><input form="${fid}" name="name" value="${fn:escapeXml(s.name)}" required maxlength="100"></td>
            <td><input form="${fid}" name="hotline" value="${fn:escapeXml(s.hotline)}" maxlength="20" class="narrow"></td>
            <td><input form="${fid}" name="baseFee" type="number" min="0" step="1000" value="<fmt:formatNumber value='${s.baseFee}' pattern='0' groupingUsed='false'/>" required class="narrow"></td>
            <td><input form="${fid}" name="estimatedDays" type="number" min="1" max="60" value="${s.estimatedDays}" required class="tiny"></td>
            <td><input form="${fid}" name="logoUrl" value="${fn:escapeXml(s.logoUrl)}" maxlength="255"></td>
            <td><input form="${fid}" name="active" type="checkbox" value="true" ${s.active ? 'checked' : ''} aria-label="Đang dùng"></td>
            <td>
              <form method="post" id="${fid}">
                <input type="hidden" name="id" value="${fn:escapeXml(s.id)}">
                <button name="action" value="save" class="light">Lưu</button>
              </form>
            </td>
          </tr>
        </c:forEach>
      </tbody>
    </table>
  </div>
</div>
<%@ include file="/WEB-INF/views/common/footer.jsp" %>
