<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Hồ sơ cá nhân" />
<c:set var="pageCss" value="account.css" />
<jsp:include page="/WEB-INF/views/common/header.jsp" />

<section class="account-page">
    <div class="account-heading">
        <div>
            <p class="account-eyebrow">TÀI KHOẢN CỦA BẠN</p>
            <h1>Hồ sơ cá nhân</h1>
            <p class="account-subtitle">Cập nhật thông tin, địa chỉ giao hàng và tài khoản nhận tiền bán hàng.</p>
        </div>
        <form action="${pageContext.request.contextPath}/logout" method="post">
            <button class="account-button account-button-light" type="submit">Đăng xuất</button>
        </form>
    </div>

    <c:if test="${not empty profileError}">
        <div class="account-alert account-alert-error"><c:out value="${profileError}" /></div>
    </c:if>
    <c:if test="${param.saved == '1'}">
        <div class="account-alert account-alert-success">Hồ sơ đã được cập nhật.</div>
    </c:if>
    <c:if test="${param.avatar == 'updated'}">
        <div class="account-alert account-alert-success">Ảnh đại diện đã được cập nhật.</div>
    </c:if>
    <c:if test="${param.avatar == 'invalid'}">
        <div class="account-alert account-alert-error">Ảnh không hợp lệ hoặc vượt quá 5 MB. Hãy chọn ảnh PNG hoặc JPEG.</div>
    </c:if>
    <c:if test="${param.avatar == 'saveFailed'}">
        <div class="account-alert account-alert-error">Không lưu được ảnh đại diện. Vui lòng thử lại.</div>
    </c:if>
    <c:if test="${param.password == 'changed'}">
        <div class="account-alert account-alert-success">Mật khẩu đã được đổi.</div>
    </c:if>
    <c:if test="${param.password == 'incorrect'}">
        <div class="account-alert account-alert-error">Mật khẩu hiện tại chưa đúng.</div>
    </c:if>
    <c:if test="${param.password == 'invalid'}">
        <div class="account-alert account-alert-error">Mật khẩu mới cần từ 8 ký tự và hai ô xác nhận phải trùng nhau.</div>
    </c:if>
    <c:if test="${param.address == 'addressSaved'}">
        <div class="account-alert account-alert-success">Sổ địa chỉ đã được cập nhật.</div>
    </c:if>
    <c:if test="${param.address == 'addressNotFound'}">
        <div class="account-alert account-alert-error">Không tìm thấy địa chỉ cần thao tác.</div>
    </c:if>
    <c:if test="${param.address == 'invalidAddress'}">
        <div class="account-alert account-alert-error">Vui lòng kiểm tra lại thông tin địa chỉ.</div>
    </c:if>

    <div class="account-grid">
        <section class="account-card account-profile-card">
            <div class="account-card-heading">
                <div>
                    <h2>Thông tin cá nhân</h2>
                    <p>Thông tin này gắn với tài khoản người mua và người bán của bạn.</p>
                </div>
                <c:choose>
                    <c:when test="${not empty customer.avatarPath}">
                        <img class="account-avatar" src="${pageContext.request.contextPath}<c:out value='${customer.avatarPath}'/>" alt="Ảnh đại diện">
                    </c:when>
                    <c:otherwise>
                        <img class="account-avatar" src="${pageContext.request.contextPath}/assets/images/avt.png" alt="Ảnh đại diện mặc định">
                    </c:otherwise>
                </c:choose>
            </div>

            <form action="${pageContext.request.contextPath}/account/profile" method="post" class="account-form">
                <label for="fullName">Họ và tên</label>
                <input id="fullName" name="fullName" type="text" maxlength="120" required
                       value="<c:out value='${customer.fullName}'/>">

                <label for="email">Email đăng nhập</label>
                <input id="email" type="email" readonly value="<c:out value='${customer.email}'/>">

                <label for="phone">Số điện thoại</label>
                <input id="phone" name="phone" type="tel" maxlength="30" required
                       value="<c:out value='${customer.phone}'/>">

                <label for="payoutAccountRef">Tài khoản nhận tiền</label>
                <input id="payoutAccountRef" name="payoutAccountRef" type="text" maxlength="255"
                       placeholder="Ví dụ: ngân hàng, số tài khoản, tên chủ tài khoản"
                       value="<c:out value='${customer.payoutAccountRef}'/>">

                <button class="account-button" type="submit">Lưu thay đổi</button>
            </form>

            <form action="${pageContext.request.contextPath}/account/avatar" method="post"
                  enctype="multipart/form-data" class="account-form avatar-form">
                <label for="avatar">Ảnh đại diện (PNG/JPEG, tối đa 5 MB)</label>
                <div class="avatar-input-row">
                    <input id="avatar" name="avatar" type="file" accept="image/png,image/jpeg" required>
                    <button class="account-button account-button-light" type="submit">Tải ảnh lên</button>
                </div>
            </form>
        </section>

        <section class="account-card">
            <h2>Đổi mật khẩu</h2>
            <p>Dùng mật khẩu hiện tại để xác nhận thay đổi.</p>
            <form action="${pageContext.request.contextPath}/account/password" method="post" class="account-form">
                <label for="currentPassword">Mật khẩu hiện tại</label>
                <input id="currentPassword" name="currentPassword" type="password" autocomplete="current-password" required>

                <label for="newPassword">Mật khẩu mới</label>
                <input id="newPassword" name="newPassword" type="password" minlength="8" maxlength="128"
                       autocomplete="new-password" required>

                <label for="confirmPassword">Nhập lại mật khẩu mới</label>
                <input id="confirmPassword" name="confirmPassword" type="password" minlength="8" maxlength="128"
                       autocomplete="new-password" required>

                <button class="account-button" type="submit">Đổi mật khẩu</button>
            </form>
        </section>

        <section class="account-card account-address-card">
            <div class="account-card-heading">
                <div>
                    <h2>Sổ địa chỉ</h2>
                    <p>Địa chỉ mặc định sẽ được ưu tiên khi đặt hàng.</p>
                </div>
            </div>

            <c:choose>
                <c:when test="${empty addresses}">
                    <p class="account-empty">Bạn chưa thêm địa chỉ giao hàng nào.</p>
                </c:when>
                <c:otherwise>
                    <div class="address-list">
                        <c:forEach var="address" items="${addresses}">
                            <article class="address-item ${address.defaultAddress ? 'is-default' : ''}">
                                <div class="address-item-heading">
                                    <div>
                                        <strong><c:out value="${address.label}" /></strong>
                                        <c:if test="${address.defaultAddress}"><span class="default-badge">MẶC ĐỊNH</span></c:if>
                                    </div>
                                    <span><c:out value="${address.recipientName}" /> · <c:out value="${address.phone}" /></span>
                                </div>
                                <p class="address-text">
                                    <c:out value="${address.addressLine}" />,
                                    <c:if test="${not empty address.ward}"><c:out value="${address.ward}" />, </c:if>
                                    <c:if test="${not empty address.district}"><c:out value="${address.district}" />, </c:if>
                                    <c:out value="${address.province}" />
                                </p>

                                <details class="address-edit">
                                    <summary>Sửa địa chỉ</summary>
                                    <form action="${pageContext.request.contextPath}/account/addresses" method="post" class="account-form compact-form">
                                        <input type="hidden" name="action" value="update">
                                        <input type="hidden" name="id" value="<c:out value='${address.id}'/>">
                                        <label>Nhãn địa chỉ<input name="label" maxlength="50" value="<c:out value='${address.label}'/>" required></label>
                                        <label>Địa chỉ cụ thể<input name="addressLine" maxlength="255" value="<c:out value='${address.addressLine}'/>" required></label>
                                        <label>Phường/xã<input name="ward" maxlength="100" value="<c:out value='${address.ward}'/>" /></label>
                                        <label>Quận/huyện<input name="district" maxlength="100" value="<c:out value='${address.district}'/>" /></label>
                                        <label>Tỉnh/thành phố<input name="province" maxlength="100" value="<c:out value='${address.province}'/>" required></label>
                                        <button class="account-button" type="submit">Lưu địa chỉ</button>
                                    </form>
                                </details>

                                <div class="address-actions">
                                    <c:if test="${not address.defaultAddress}">
                                        <form action="${pageContext.request.contextPath}/account/addresses" method="post">
                                            <input type="hidden" name="action" value="default">
                                            <input type="hidden" name="id" value="<c:out value='${address.id}'/>">
                                            <button class="text-button" type="submit">Đặt làm mặc định</button>
                                        </form>
                                    </c:if>
                                    <form action="${pageContext.request.contextPath}/account/addresses" method="post"
                                          onsubmit="return confirm('Bạn có chắc muốn xoá địa chỉ này?');">
                                        <input type="hidden" name="action" value="delete">
                                        <input type="hidden" name="id" value="<c:out value='${address.id}'/>">
                                        <button class="text-button danger-text" type="submit">Xoá</button>
                                    </form>
                                </div>
                            </article>
                        </c:forEach>
                    </div>
                </c:otherwise>
            </c:choose>

            <details class="add-address" open>
                <summary>Thêm địa chỉ mới</summary>
                <form action="${pageContext.request.contextPath}/account/addresses" method="post" class="account-form compact-form">
                    <input type="hidden" name="action" value="add">
                    <label>Nhãn địa chỉ<input name="label" maxlength="50" placeholder="Nhà, ký túc xá..." value="Nhà" required></label>
                    <label>Địa chỉ cụ thể<input name="addressLine" maxlength="255" placeholder="Số nhà, tên đường" required></label>
                    <label>Phường/xã<input name="ward" maxlength="100"></label>
                    <label>Quận/huyện<input name="district" maxlength="100"></label>
                    <label>Tỉnh/thành phố<input name="province" maxlength="100" required></label>
                    <button class="account-button" type="submit">Thêm địa chỉ</button>
                </form>
            </details>
        </section>
    </div>
</section>

<jsp:include page="/WEB-INF/views/common/footer.jsp" />
