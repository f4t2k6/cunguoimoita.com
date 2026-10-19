<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN" scope="request"/>
<%-- Quản lý voucher. Dữ liệu từ AdminVoucherServlet: vouchers (List<VoucherRow>: voucher, ownerEmail, ownerName) --%>
<c:set var="pageTitle" value="Voucher"/>
<c:set var="pageCss" value="admin.css"/>
<c:set var="pageMainClass" value="page-wide"/>
<%@ include file="/WEB-INF/views/common/header.jsp" %>
<div class="admin-page page-card">
  <div class="page-card-head">
    <p class="page-eyebrow">QUẢN LÝ</p>
    <h1 class="page-title">Voucher</h1>
  </div>
  <%@ include file="/WEB-INF/views/admin/_menu.jsp" %>
  <p class="admin-note">Mỗi voucher thuộc một khách và dùng được 1 lần. Để trống email thì mỗi khách đang hoạt động nhận 1 voucher,
    mã = mã bạn nhập + 4 ký tự ngẫu nhiên. Khách được báo qua chuông thông báo và thấy mã ở trang đơn hàng.</p>

  <form method="post" class="admin-card admin-form">
    <h2>Phát voucher</h2>
    <label>Mã <input name="code" required maxlength="30" pattern="[A-Za-z0-9-]{3,30}" placeholder="VD: GIAM30K"></label>
    <label>Email khách (trống = tất cả) <input name="email" type="email" maxlength="150" placeholder="b1@cunguoimoita.com"></label>
    <label>Giảm (đ) <input name="discountValue" type="number" min="1000" step="1000" required></label>
    <label>Đơn tối thiểu (đ) <input name="minOrderValue" type="number" min="0" step="1000" value="0"></label>
    <label>Hết hạn <input name="expiryDate" type="date"></label>
    <button type="submit">Phát</button>
  </form>

  <div class="admin-table-wrap">
    <table class="admin-table">
      <thead><tr><th>Mã</th><th>Khách</th><th>Giảm</th><th>Đơn tối thiểu</th><th>Phát ngày</th><th>Hết hạn</th><th>Trạng thái</th></tr></thead>
      <tbody>
        <c:forEach var="row" items="${vouchers}">
          <c:set var="v" value="${row.voucher()}"/>
          <tr class="${v.status == 'ISSUED' ? '' : 'is-off'}">
            <td><strong><c:out value="${v.code}"/></strong></td>
            <td><c:out value="${row.ownerName()}"/><br><small><c:out value="${row.ownerEmail()}"/></small></td>
            <td><fmt:formatNumber value="${v.discountValue}" pattern="#,##0"/> đ</td>
            <td><fmt:formatNumber value="${v.minOrderValue}" pattern="#,##0"/> đ</td>
            <td>${v.issuedDate}</td>
            <td>${empty v.expiryDate ? 'Không hạn' : v.expiryDate}</td>
            <td>${v.status == 'ISSUED' ? 'Chưa dùng' : v.status == 'USED' ? 'Đã dùng' : 'Hết hạn'}</td>
          </tr>
        </c:forEach>
      </tbody>
    </table>
  </div>
</div>
<%@ include file="/WEB-INF/views/common/footer.jsp" %>
