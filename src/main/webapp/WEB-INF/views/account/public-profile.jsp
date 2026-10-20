<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="pageTitle" value="Hồ sơ người bán" />
<c:set var="pageCss" value="account.css" />
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<section class="account-page public-profile-page page-card">
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
                <span class="seller-stat-value"><span class="tier-pill tier-${seller.memberTier}"><c:out value="${seller.memberTier}" /></span></span>
                <span class="seller-stat-label">Hạng thành viên</span>
            </div>
        </div>
        <p class="public-profile-note">Điểm đánh giá, số đơn đã bán và hạng thành viên được tổng hợp từ các đơn đã hoàn tất.</p>
        <c:if test="${not isSelf}">
            <a class="public-report-link" href="${ctx}/report?type=user&amp;id=${fn:escapeXml(param.id)}">Báo cáo người dùng</a>
        </c:if>
    </div>

    <%-- Đánh giá nhận được --%>
    <section class="detail-section">
        <h2 class="section-heading">Đánh giá (${fn:length(reviews)})</h2>
        <c:choose>
            <c:when test="${empty reviews}"><p class="muted">Chưa có đánh giá nào.</p></c:when>
            <c:otherwise>
                <ul class="public-reviews">
                    <c:forEach var="r" items="${reviews}">
                        <li>
                            <p><strong><c:out value="${r.authorName()}"/></strong>
                               <span class="public-review-stars" aria-label="${r.rating()} trên 5 sao"><c:forEach begin="1" end="${r.rating()}">★</c:forEach><c:forEach begin="${r.rating() + 1}" end="5">☆</c:forEach></span>
                               <span class="muted">${r.createdDate().toLocalDate()}</span></p>
                            <c:if test="${not empty r.content()}"><p><c:out value="${r.content()}"/></p></c:if>
                        </li>
                    </c:forEach>
                </ul>
            </c:otherwise>
        </c:choose>
    </section>

    <%-- Bài đang bán của người này; thẻ nhỏ dùng chung với trang chi tiết bài --%>
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
