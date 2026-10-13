<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Hồ sơ người bán" />
<c:set var="pageCss" value="account.css" />
<jsp:include page="/WEB-INF/views/common/header.jsp" />

<section class="account-page public-profile-page">
    <p class="account-eyebrow">HỒ SƠ NGƯỜI BÁN</p>
    <div class="account-card public-seller-card">
        <c:choose>
            <c:when test="${not empty seller.avatarPath}">
                <img class="account-avatar public-avatar" src="${pageContext.request.contextPath}<c:out value='${seller.avatarPath}'/>" alt="Ảnh đại diện người bán">
            </c:when>
            <c:otherwise>
                <img class="account-avatar public-avatar" src="${pageContext.request.contextPath}/assets/images/avt.png" alt="Ảnh đại diện mặc định">
            </c:otherwise>
        </c:choose>
        <h1><c:out value="${seller.fullName}" /></h1>
        <p class="public-seller-id">Thành viên Cunguoimoita</p>

        <div class="seller-stat-grid">
            <div class="seller-stat">
                <span class="seller-stat-value">⭐ <c:out value="${seller.averageRating}" /> / 5</span>
                <span class="seller-stat-label">Điểm đánh giá</span>
            </div>
            <div class="seller-stat">
                <span class="seller-stat-value"><c:out value="${seller.totalSales}" /></span>
                <span class="seller-stat-label">Đơn đã bán</span>
            </div>
            <div class="seller-stat">
                <span class="seller-stat-value"><c:out value="${seller.memberTier}" /></span>
                <span class="seller-stat-label">Hạng thành viên</span>
            </div>
        </div>
        <p class="public-profile-note">Điểm đánh giá, số đơn đã bán và hạng thành viên được tổng hợp từ dữ liệu F8.</p>
    </div>
</section>

<jsp:include page="/WEB-INF/views/common/footer.jsp" />
