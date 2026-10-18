<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%-- Hạng thành viên. Dữ liệu từ ReviewServlet:
     progress (TierProgress: points, current, next, percent), summary (RatingSummary), reviews (List<ReviewLine>) --%>
<c:set var="pageTitle" value="Hạng thành viên"/>
<c:set var="pageCss" value="order.css"/>
<%@ include file="/WEB-INF/views/common/header.jsp" %>
<c:set var="p" value="${progress}"/>
<div class="order-page">
  <h1>Hạng thành viên</h1>

  <%-- ========== HUY HIỆU + THANH TIẾN ĐỘ ========== --%>
  <section class="order-card">
    <p class="tier-badge tier-${p.current().name}">${p.current().name}</p>
    <p><strong>${p.points()}</strong> ecoPoints
       <span class="hint">(10.000đ giao dịch hoàn tất = 1 điểm)</span></p>

    <c:choose>
      <c:when test="${p.next() != null}">
        <%-- width = % tiến độ → thanh đen dài tương ứng --%>
        <div class="progress"><div class="progress-bar" style="width: ${p.percent()}%"></div></div>
        <p class="hint">Còn ${p.next().minEcoPoints - p.points()} điểm nữa để lên <strong>${p.next().name}</strong></p>
      </c:when>
      <c:otherwise><p class="hint">Bạn đã đạt hạng cao nhất 🎉</p></c:otherwise>
    </c:choose>

    <p>Ưu đãi hiện tại: <c:out value="${p.current().benefitNote}"/></p>
  </section>

  <%-- ========== ĐIỂM ĐÁNH GIÁ ========== --%>
  <section class="order-card">
    <h2>Đánh giá về bạn</h2>
    <p>
      Điểm trung bình: <strong><fmt:formatNumber value="${summary.avgRating()}" pattern="0.0"/> ★</strong>
      · Đã bán: <strong>${summary.totalSales()}</strong> đơn
    </p>

    <c:choose>
      <c:when test="${empty reviews}"><p class="hint">Chưa có đánh giá nào.</p></c:when>
      <c:otherwise>
        <c:forEach var="r" items="${reviews}">
          <div class="review-item">
            <strong><c:out value="${r.authorName()}"/></strong>
            <span class="stars-text">
              <%-- In r.rating() ngôi sao đặc --%>
              <c:forEach begin="1" end="${r.rating()}">★</c:forEach>
            </span>
            <span class="hint">${r.createdDate().toLocalDate()}</span>
            <c:if test="${not empty r.content()}"><p><c:out value="${r.content()}"/></p></c:if>
          </div>
        </c:forEach>
      </c:otherwise>
    </c:choose>
  </section>
</div>
<%@ include file="/WEB-INF/views/common/footer.jsp" %>