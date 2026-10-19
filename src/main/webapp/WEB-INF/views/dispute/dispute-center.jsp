<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN" scope="request"/>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%-- Khiếu nại & hoàn tiền (F9). Dữ liệu từ DisputeServlet:
     userDisputes, userRefunds, currentDispute (khi ?id=), order (khi ?id= hoặc ?orderId=), openProblem, reasons, userId, error, message --%>
<c:set var="pageTitle" value="Khiếu nại & hoàn tiền"/>
<c:set var="pageCss" value="order.css"/>
<c:set var="backUrl" value="${not empty order ? '/order/detail?id='.concat(order.id) : '/order/list'}"/>
<%@ include file="/WEB-INF/views/common/header.jsp" %>
<div class="order-page">
  <h1>Khiếu nại &amp; hoàn tiền</h1>

  <nav class="order-tabs">
    <a href="${ctx}/order/cart">Giỏ hàng</a>
    <a href="${ctx}/order/list?as=buyer">Đơn mua</a>
    <a href="${ctx}/order/list?as=seller">Đơn bán</a>
    <a href="${ctx}/seller/payouts">Thu nhập bán hàng</a>
    <a href="${ctx}/dispute" class="active">Khiếu nại &amp; hoàn tiền</a>
    <a href="${ctx}/review/tier">Hạng thành viên</a>
  </nav>

  <c:if test="${not empty error}"><p class="order-alert error"><c:out value="${error}"/></p></c:if>
  <c:if test="${not empty message}"><p class="order-alert"><c:out value="${message}"/></p></c:if>

  <c:set var="d" value="${currentDispute}"/>
  <c:choose>
    <%-- ========== CHI TIẾT 1 KHIẾU NẠI ========== --%>
    <c:when test="${not empty d}">
      <section class="order-card">
        <h2>Khiếu nại đơn <a href="${ctx}/order/detail?id=${d.orderId}"><c:out value="${order.code}"/></a>
          <span class="status ${d.open ? '' : 'status-COMPLETED'}">${d.status.displayName}</span></h2>
        <p class="hint">Mở ngày ${d.createdDate} · Lý do: <strong>${d.reason.displayName}</strong>
          · Bạn là <strong>${userId == d.raisedById ? 'người mua' : (userId == d.againstId ? 'người bán' : 'admin')}</strong></p>

        <h3>Người mua trình bày</h3>
        <p class="pre-line"><c:out value="${d.description}"/></p>
        <div class="evidence-grid">
          <c:forEach var="img" items="${d.evidenceList}">
            <a href="${ctx}${img}" target="_blank"><img src="${ctx}${img}" alt="Ảnh bằng chứng của người mua"></a>
          </c:forEach>
        </div>

        <h3>Người bán phản hồi</h3>
        <c:choose>
          <c:when test="${empty d.sellerResponse}"><p class="hint">Người bán chưa phản hồi.</p></c:when>
          <c:otherwise>
            <p class="pre-line"><c:out value="${d.sellerResponse}"/></p>
            <div class="evidence-grid">
              <c:forEach var="img" items="${d.sellerEvidenceList}">
                <a href="${ctx}${img}" target="_blank"><img src="${ctx}${img}" alt="Ảnh bằng chứng của người bán"></a>
              </c:forEach>
            </div>
          </c:otherwise>
        </c:choose>

        <c:if test="${not empty d.resolutionNote}">
          <h3>Kết quả của admin</h3>
          <p class="pre-line"><c:out value="${d.resolutionNote}"/> <span class="hint">(${d.resolvedDate})</span></p>
        </c:if>

        <%-- Người bán gửi / sửa phản hồi khi khiếu nại còn mở --%>
        <c:if test="${d.open and userId == d.againstId}">
          <form method="post" action="${ctx}/dispute/respond" enctype="multipart/form-data" class="stack-form">
            <input type="hidden" name="disputeId" value="${fn:escapeXml(d.id)}">
            <label>Lời giải thích của bạn
              <textarea name="response" rows="3" maxlength="2000" required><c:out value="${d.sellerResponse}"/></textarea>
            </label>
            <label>Ảnh bằng chứng (tối đa 5 ảnh, mỗi ảnh ≤ 5 MB)
              <input type="file" name="images" accept="image/*" multiple>
            </label>
            <button>Gửi phản hồi</button>
          </form>
        </c:if>
        <%-- Người mua nộp thêm ảnh --%>
        <c:if test="${d.open and userId == d.raisedById}">
          <form method="post" action="${ctx}/dispute/evidence" enctype="multipart/form-data" class="stack-form">
            <input type="hidden" name="disputeId" value="${fn:escapeXml(d.id)}">
            <label>Nộp thêm ảnh bằng chứng <input type="file" name="images" accept="image/*" multiple required></label>
            <button class="secondary">Gửi ảnh</button>
          </form>
        </c:if>
      </section>
    </c:when>

    <%-- ========== FORM MỞ KHIẾU NẠI CHO 1 ĐƠN ========== --%>
    <c:when test="${not empty order}">
      <section class="order-card">
        <h2>Khiếu nại đơn <a href="${ctx}/order/detail?id=${order.id}"><c:out value="${order.code}"/></a></h2>
        <c:choose>
          <c:when test="${not empty openProblem}"><p class="order-alert error"><c:out value="${openProblem}"/></p></c:when>
          <c:otherwise>
            <p class="hint">Trong lúc khiếu nại chưa được xử lý, đơn sẽ không tự hoàn tất và tiền chưa chuyển cho người bán.</p>
            <form method="post" action="${ctx}/dispute/create" enctype="multipart/form-data" class="stack-form">
              <input type="hidden" name="orderId" value="${fn:escapeXml(order.id)}">
              <label>Lý do
                <select name="reason">
                  <c:forEach var="r" items="${reasons}">
                    <option value="${r.dbValue()}">${r.displayName}</option>
                  </c:forEach>
                </select>
              </label>
              <label>Mô tả vấn đề
                <textarea name="description" rows="4" maxlength="2000" required placeholder="VD: Máy không lên nguồn khi nhận hàng"></textarea>
              </label>
              <label>Ảnh bằng chứng (tối đa 5 ảnh, mỗi ảnh ≤ 5 MB)
                <input type="file" name="images" accept="image/*" multiple>
              </label>
              <button>Gửi khiếu nại</button>
            </form>
          </c:otherwise>
        </c:choose>
      </section>
    </c:when>
  </c:choose>

  <%-- ========== DANH SÁCH ========== --%>
  <section class="order-card">
    <h2>Khiếu nại của tôi</h2>
    <c:choose>
      <c:when test="${empty userDisputes}">
        <p class="hint">Chưa có khiếu nại nào. Muốn khiếu nại, mở đơn hàng đang giao hoặc đã giao và bấm "Khiếu nại".</p>
      </c:when>
      <c:otherwise>
        <table class="order-table">
          <tr><th>Ngày</th><th>Lý do</th><th>Vai trò</th><th>Trạng thái</th><th></th></tr>
          <c:forEach var="x" items="${userDisputes}">
            <tr>
              <td>${x.createdDate}</td>
              <td>${x.reason.displayName}</td>
              <td>${x.raisedById == userId ? 'Tôi khiếu nại' : 'Bị khiếu nại'}</td>
              <td>${x.status.displayName}</td>
              <td><a href="${ctx}/dispute?id=${x.id}">Xem</a></td>
            </tr>
          </c:forEach>
        </table>
      </c:otherwise>
    </c:choose>
  </section>

  <section class="order-card">
    <h2>Khoản được hoàn tiền</h2>
    <c:choose>
      <c:when test="${empty userRefunds}"><p class="hint">Chưa có khoản hoàn tiền nào.</p></c:when>
      <c:otherwise>
        <table class="order-table">
          <tr><th>Ngày</th><th>Đơn</th><th>Lý do</th><th>Số tiền</th><th>Trạng thái</th></tr>
          <c:forEach var="r" items="${userRefunds}">
            <tr>
              <td>${r.refundDate.toLocalDate()}</td>
              <td><a href="${ctx}/order/detail?id=${r.orderId}">Xem đơn</a></td>
              <td>${r.reason.displayName}</td>
              <td><fmt:formatNumber value="${r.amount}" pattern="#,##0"/> đ</td>
              <td>${r.status.displayName}</td>
            </tr>
          </c:forEach>
        </table>
      </c:otherwise>
    </c:choose>
  </section>
</div>
<%@ include file="/WEB-INF/views/common/footer.jsp" %>
