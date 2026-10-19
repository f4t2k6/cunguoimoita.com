<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="Khiếu nại"/>
<c:set var="pageCss" value="admin.css"/>
<c:set var="pageMainClass" value="page-wide"/>
<%@ include file="/WEB-INF/views/common/header.jsp" %>
<div class="admin-page page-card">
  <div class="page-card-head">
    <p class="page-eyebrow">TRỌNG TÀI</p>
    <h1 class="page-title">Khiếu nại</h1>
  </div>
  <%@ include file="/WEB-INF/views/admin/_menu.jsp" %>
  <p class="admin-note">Xem trình bày và ảnh của hai bên rồi chọn: hoàn tiền cho người mua (đơn chuyển "Đã hoàn tiền")
    hoặc xử cho người bán (đơn hoàn tất, tiền chuyển cho người bán đã trừ phí sàn).</p>

  <c:if test="${empty allDisputes}">
    <p class="admin-empty">Hiện không có hồ sơ khiếu nại nào trong hệ thống.</p>
  </c:if>

  <div class="admin-cards">
    <c:forEach var="d" items="${allDisputes}">
      <article class="admin-card">
        <header>
          <h2>Đơn ORD-${fn:toUpperCase(fn:substring(d.orderId, 0, 8))}
            <small>· <a href="${ctx}/dispute?id=${d.id}">xem chi tiết</a></small></h2>
          <span class="admin-badge ${d.open ? 'warn' : d.status.dbValue() == 'resolved_refund' ? 'ok' : 'off'}">
            ${d.status.displayName}
          </span>
        </header>

        <p class="admin-meta">
          Lý do: <strong>${d.reason.displayName}</strong> · Người mua: <code>${d.raisedById}</code>
          · Người bán: <code>${d.againstId}</code> · Ngày tạo: ${d.createdDate}
        </p>

        <p class="admin-desc"><strong>Người mua:</strong> <c:out value="${d.description}"/></p>
        <div class="admin-evidence">
          <c:forEach var="img" items="${d.evidenceList}">
            <a href="${ctx}${img}" target="_blank"><img src="${ctx}${img}" alt="Ảnh của người mua"></a>
          </c:forEach>
        </div>

        <c:choose>
          <c:when test="${empty d.sellerResponse}"><p class="admin-meta">Người bán chưa phản hồi.</p></c:when>
          <c:otherwise>
            <div class="admin-quote"><strong>Người bán:</strong> <c:out value="${d.sellerResponse}"/></div>
            <div class="admin-evidence">
              <c:forEach var="img" items="${d.sellerEvidenceList}">
                <a href="${ctx}${img}" target="_blank"><img src="${ctx}${img}" alt="Ảnh của người bán"></a>
              </c:forEach>
            </div>
          </c:otherwise>
        </c:choose>

        <c:if test="${not empty d.resolutionNote}">
          <div class="admin-quote"><strong>Kết quả:</strong> <c:out value="${d.resolutionNote}"/>
            <c:if test="${not empty d.resolvedDate}"> · ${d.resolvedDate}</c:if></div>
        </c:if>

        <c:if test="${d.open}">
          <div class="admin-actions">
            <c:if test="${d.status.dbValue() == 'open'}">
              <form method="post" action="${ctx}/admin/disputes" class="admin-inline">
                <input type="hidden" name="disputeId" value="${d.id}">
                <input type="hidden" name="action" value="review">
                <button type="submit" class="light">Tiếp nhận, đang xem xét</button>
              </form>
            </c:if>
            <form method="post" action="${ctx}/admin/disputes" class="admin-inline"
                  onsubmit="return confirm('Hoàn tiền cho người mua?');">
              <input type="hidden" name="disputeId" value="${d.id}">
              <input type="hidden" name="action" value="resolve_buyer">
              <input type="text" name="resolutionNote" placeholder="Lý do chấp nhận khiếu nại" required>
              <button type="submit">Hoàn tiền người mua</button>
            </form>
            <form method="post" action="${ctx}/admin/disputes" class="admin-inline"
                  onsubmit="return confirm('Xử cho người bán và hoàn tất đơn?');">
              <input type="hidden" name="disputeId" value="${d.id}">
              <input type="hidden" name="action" value="resolve_seller">
              <input type="text" name="resolutionNote" placeholder="Lý do bác khiếu nại" required>
              <button type="submit">Xử cho người bán</button>
            </form>
            <form method="post" action="${ctx}/admin/disputes" class="admin-inline">
              <input type="hidden" name="disputeId" value="${d.id}">
              <input type="hidden" name="action" value="reject">
              <input type="hidden" name="resolutionNote" value="Không đủ căn cứ khiếu nại">
              <button type="submit" class="light">Bác bỏ (đơn tiếp tục)</button>
            </form>
          </div>
        </c:if>
      </article>
    </c:forEach>
  </div>
</div>
<%@ include file="/WEB-INF/views/common/footer.jsp" %>
