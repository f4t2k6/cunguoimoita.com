<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="Danh mục"/>
<c:set var="pageCss" value="admin.css"/>
<c:set var="pageMainClass" value="page-wide"/>
<%@ include file="/WEB-INF/views/common/header.jsp" %>
<div class="admin-page page-card">
  <div class="page-card-head">
    <p class="page-eyebrow">QUẢN LÝ</p>
    <h1 class="page-title">Danh mục</h1>
  </div>
  <%@ include file="/WEB-INF/views/admin/_menu.jsp" %>

  <form method="post" class="admin-card admin-form">
    <h2>Thêm danh mục</h2>
    <label>Tên <input name="name" required maxlength="100"></label>
    <label>Thuộc danh mục
      <select name="parentId">
        <option value="">(Danh mục gốc)</option>
        <c:forEach var="p" items="${categories}">
          <c:if test="${p.topLevel}"><option value="${fn:escapeXml(p.id)}"><c:out value="${p.name}"/></option></c:if>
        </c:forEach>
      </select>
    </label>
    <label class="wide">Mô tả <input name="description" maxlength="500"></label>
    <button type="submit">Thêm</button>
  </form>

  <div class="admin-table-wrap">
    <table class="admin-table">
      <thead><tr><th>Tên</th><th>Thuộc danh mục</th><th>Mô tả</th><th>Số bài</th><th></th></tr></thead>
      <tbody>
        <c:forEach var="cg" items="${categories}">
          <tr class="${cg.topLevel ? '' : 'is-child'}">
            <%-- Mỗi dòng là một form sửa riêng (thuộc tính form= gắn ô nhập vào form của dòng) --%>
            <td><input form="cg-${fn:escapeXml(cg.id)}" name="name" value="${fn:escapeXml(cg.name)}" required maxlength="100"></td>
            <td>
              <select form="cg-${fn:escapeXml(cg.id)}" name="parentId">
                <option value="">(Danh mục gốc)</option>
                <c:forEach var="p" items="${categories}">
                  <c:if test="${p.topLevel and p.id != cg.id}">
                    <option value="${fn:escapeXml(p.id)}" ${p.id == cg.parentCategoryId ? 'selected' : ''}><c:out value="${p.name}"/></option>
                  </c:if>
                </c:forEach>
              </select>
            </td>
            <td><input form="cg-${fn:escapeXml(cg.id)}" name="description" value="${fn:escapeXml(cg.description)}" maxlength="500"></td>
            <td>${cg.productCount}</td>
            <td>
              <form method="post" id="cg-${fn:escapeXml(cg.id)}">
                <input type="hidden" name="id" value="${fn:escapeXml(cg.id)}">
                <button type="submit" class="light">Lưu</button>
              </form>
            </td>
          </tr>
        </c:forEach>
      </tbody>
    </table>
  </div>
</div>
<%@ include file="/WEB-INF/views/common/footer.jsp" %>
