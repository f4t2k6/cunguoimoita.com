<%@ page pageEncoding="UTF-8" %>
<%-- Thông báo kết quả thao tác (flash) của khu quản trị. Include tĩnh ở đầu mỗi trang admin.
     Menu chuyển trang quản trị nằm ở menu trái (header.jsp, khi đăng nhập admin). --%>
<c:if test="${not empty flash}"><p class="admin-flash" role="status"><c:out value="${flash}"/></p></c:if>
<c:if test="${not empty flashError}"><p class="admin-flash error" role="alert"><c:out value="${flashError}"/></p></c:if>
