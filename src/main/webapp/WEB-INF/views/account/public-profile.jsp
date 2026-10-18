<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="pageTitle" value="Hồ sơ người bán" />
<c:set var="pageCss" value="account.css" />
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<section class="account-page public-profile-page">
    <p class="account-eyebrow">HỒ SƠ NGƯỜI BÁN</p>
    <div class="account-card public-seller-card">
        <c:choose>
            <c:when test="${not empty seller.avatarPath}">
                <img class="account-avatar public-avatar" src="${pageContext.request.contextPath}<c:out value='${seller.avatarPath}'/>" alt="Ảnh đại diện người bán">
            </c:when>
            <c:otherwise>
                <img class="account-avatar public-avatar" src="${pageContext.request.contextPath}/assets/images/default-avatar.png" alt="Ảnh đại diện mặc định">
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

    <%-- Bài đang bán của người này (F2); thẻ nhỏ dùng chung với trang chi tiết bài --%>
    <link rel="stylesheet" href="${ctx}/assets/css/product.css?v=${assetVer}">
    <section class="detail-section">
        <h2 class="section-heading">Đang bán (${fn:length(sellerProducts)})</h2>
        <c:choose>
            <c:when test="${empty sellerProducts}"><p class="muted">Người này chưa có bài nào đang bán.</p></c:when>
            <c:otherwise>
                <div class="mini-grid">
                    <c:forEach var="m" items="${sellerProducts}"><%@ include file="/WEB-INF/views/product/_mini-card.jsp" %></c:forEach>
                </div>
            </c:otherwise>
        </c:choose>
    </section>
</section>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
