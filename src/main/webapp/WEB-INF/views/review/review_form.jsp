<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Form đánh giá. Dữ liệu từ ReviewServlet: orderId, error --%>
<c:set var="pageTitle" value="Đánh giá giao dịch"/>
<c:set var="pageCss" value="order.css"/>
<%@ include file="/WEB-INF/views/common/header.jsp" %>
<div class="order-page">
  <h1>Đánh giá giao dịch</h1>
  <c:if test="${not empty error}"><p class="order-alert error"><c:out value="${error}"/></p></c:if>

  <form method="post" action="${ctx}/review/create" class="order-card">
    <input type="hidden" name="orderId" value="${fn:escapeXml(orderId)}">

    <p><strong>Chấm điểm</strong></p>
    <%-- 5 nút radio cùng name="rating" → chỉ chọn được 1; value là số sao gửi lên Servlet --%>
    <div class="stars">
      <c:forEach var="i" begin="1" end="5">
        <label><input type="radio" name="rating" value="${i}" required> ${i} ★</label>
      </c:forEach>
    </div>

    <p><label for="content"><strong>Nhận xét</strong> (không bắt buộc)</label></p>
    <textarea id="content" name="content" rows="4" maxlength="1000"
              placeholder="Hàng đúng mô tả, giao nhanh..."></textarea>

    <p>
      <button>Gửi đánh giá</button>
      <a href="${ctx}/order/detail?id=${fn:escapeXml(orderId)}">Huỷ</a>
    </p>
  </form>
</div>
<%@ include file="/WEB-INF/views/common/footer.jsp" %>