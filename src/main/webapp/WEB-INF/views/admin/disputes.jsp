<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="pageTitle" value="Quản lý Khiếu nại & Trọng tài"/>
<c:set var="pageCss" value="admin.css"/>
<c:set var="pageMainClass" value="page-wide"/>
<%@ include file="/WEB-INF/views/common/header.jsp" %>
<div class="admin-page">
  <%@ include file="/WEB-INF/views/admin/_menu.jsp" %>
  <h1>Toà Án Trọng Tài &amp; Xử Lý Khiếu Nại (F10 &rarr; F9)</h1>
  <p style="color: #64748b; margin-top: -6px; margin-bottom: 20px;">
    Thẩm định tranh chấp giữa người mua và người bán, đưa ra phán quyết hoàn tiền (Refund) hoặc giải ngân (Payout).
  </p>

  <c:if test="${empty allDisputes}">
    <p class="admin-empty">Hiện không có hồ sơ khiếu nại nào trong hệ thống.</p>
  </c:if>

  <div class="admin-cards">
    <c:forEach var="d" items="${allDisputes}">
      <article class="admin-card">
        <header>
          <h2>Khiếu nại #${d.id} · Đơn hàng ${d.orderId}</h2>
          <span class="admin-badge ${d.status.dbValue() == 'open' or d.status.dbValue() == 'reviewing' ? 'warn' : d.status.dbValue() == 'resolved_refund' ? 'ok' : 'off'}">
            ${d.status.displayName}
          </span>
        </header>

        <p class="admin-meta">
          Lý do: <strong>${d.reason.displayName}</strong> · Người khiếu nại: <code>${d.raisedById}</code> · Người bị khiếu nại: <code>${d.againstId}</code> · Ngày tạo: ${d.createdDate}
        </p>

        <p class="admin-desc">
          <strong>Nội dung:</strong> <c:out value="${d.description}"/>
        </p>

        <c:if test="${not empty d.evidenceImages}">
          <p style="font-size: 13px; margin: 8px 0;">
            <strong>Bằng chứng đính kèm:</strong> <code><c:out value="${d.evidenceImages}"/></code>
          </p>
        </c:if>

        <c:if test="${not empty d.resolutionNote}">
          <div style="background: #f1f5f9; padding: 10px 14px; border-radius: 6px; font-size: 13px; margin: 8px 0;">
            <strong>Ghi chú phán quyết:</strong> <c:out value="${d.resolutionNote}"/>
            <c:if test="${not empty d.resolvedDate}"> · <em>Ngày: ${d.resolvedDate}</em></c:if>
          </div>
        </c:if>

        <c:if test="${d.status.dbValue() == 'open' or d.status.dbValue() == 'reviewing'}">
          <div class="admin-actions" style="margin-top: 14px; display: flex; gap: 8px; flex-wrap: wrap;">
            <!-- Phán quyết Buyer thắng -->
            <form method="post" action="${ctx}/admin/disputes" class="admin-inline" onsubmit="return confirm('Xác nhận phán quyết Người mua thắng kiện và hoàn tiền 100%?');">
              <input type="hidden" name="disputeId" value="${d.id}">
              <input type="hidden" name="action" value="resolve_buyer">
              <input type="text" name="resolutionNote" placeholder="Lý do chấp thuận khiếu nại..." style="padding: 6px 10px; font-size: 13px; border: 1px solid #cbd5e1; border-radius: 4px;" required>
              <button type="submit" style="background: #16a34a; color: white; border: none; padding: 6px 14px; border-radius: 4px; cursor: pointer; font-weight: 600;">
                ✔ Hoàn tiền Buyer (Refund)
              </button>
            </form>

            <!-- Phán quyết Seller thắng -->
            <form method="post" action="${ctx}/admin/disputes" class="admin-inline" onsubmit="return confirm('Xác nhận phán quyết Người bán thắng kiện và mở giải ngân?');">
              <input type="hidden" name="disputeId" value="${d.id}">
              <input type="hidden" name="action" value="resolve_seller">
              <input type="text" name="resolutionNote" placeholder="Lý do bác khiếu nại..." style="padding: 6px 10px; font-size: 13px; border: 1px solid #cbd5e1; border-radius: 4px;" required>
              <button type="submit" style="background: #d97706; color: white; border: none; padding: 6px 14px; border-radius: 4px; cursor: pointer; font-weight: 600;">
                🏪 Giải ngân Seller (Payout)
              </button>
            </form>

            <!-- Từ chối -->
            <form method="post" action="${ctx}/admin/disputes" class="admin-inline">
              <input type="hidden" name="disputeId" value="${d.id}">
              <input type="hidden" name="action" value="reject">
              <input type="hidden" name="resolutionNote" value="Không đủ căn cứ khiếu nại">
              <button type="submit" class="light" style="padding: 6px 12px; border: 1px solid #cbd5e1; background: white; border-radius: 4px; cursor: pointer;">
                Bác bỏ
              </button>
            </form>
          </div>
        </c:if>
      </article>
    </c:forEach>
  </div>
</div>
<%@ include file="/WEB-INF/views/common/footer.jsp" %>
