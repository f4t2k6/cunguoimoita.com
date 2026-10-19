<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Đăng nhập - Cunguoimoita</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Bungee&family=Poppins:wght@300;400;500;600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${ctx}/assets/css/login.css">
    <link rel="stylesheet" href="${ctx}/assets/css/password.css">
    <link rel="icon" type="image/png" href="${ctx}/assets/images/logo_small.png">
</head>
<body>
<header class="header">
    <a class="logo" href="${ctx}/" aria-label="Cunguoimoita">
        <img src="${ctx}/assets/images/logo.png" alt="Cunguoimoita">
    </a>
    <div class="header-title">Đăng nhập</div>
</header>

<main class="main">
    <div class="content">
        <%@ include file="/WEB-INF/views/common/auth-intro.jspf" %>

        <section class="form-card" data-forgot-url="${ctx}/forgot-password">
            <%-- Khung 1: đăng nhập --%>
            <div class="card-view" data-view="login">
                <h2>Đăng nhập</h2>
                <c:if test="${param.registered == '1'}">
                    <p class="form-success" role="status">Đăng ký thành công. Hãy đăng nhập.</p>
                </c:if>
                <c:if test="${param.banned == '1'}">
                    <p class="form-error" role="alert">Tài khoản này đã bị khóa.</p>
                </c:if>
                <c:if test="${param.loggedOut == '1'}">
                    <p class="form-success" role="status">Bạn đã đăng xuất.</p>
                </c:if>
                <c:if test="${not empty errorMessage}">
                    <p class="form-error" role="alert"><c:out value="${errorMessage}"/></p>
                </c:if>
                <p class="form-success" role="status" data-login-note hidden></p>
                <form action="${ctx}/login" method="post">
                    <label class="sr-only" for="email">Email</label>
                    <input id="email" name="email" type="email" placeholder="Email"
                           value="${fn:escapeXml(param.email)}" autocomplete="username" required>
                    <label class="sr-only" for="password">Mật khẩu</label>
                    <input id="password" name="password" type="password" placeholder="Mật khẩu"
                           autocomplete="current-password" required>
                    <div class="form-options">
                        <button class="link-button" type="button" data-go="forgot-email">Quên mật khẩu?</button>
                    </div>
                    <button class="submit" type="submit">ĐĂNG NHẬP</button>
                </form>
                <div class="divider">HOẶC</div>
                <div class="social">
                    <a class="social-button" href="${ctx}/oauth/facebook">
                        <img src="${ctx}/assets/images/facebook-icon.svg" alt="" width="18" height="18">
                        Facebook
                    </a>
                    <a class="social-button" href="${ctx}/oauth/google">
                        <img src="${ctx}/assets/images/google-icon.svg" alt="" width="18" height="18">
                        Google
                    </a>
                </div>
                <p class="terms">
                    Bằng việc đăng nhập, bạn đồng ý với
                    <a href="#">Điều khoản dịch vụ</a> và
                    <a href="#">Chính sách bảo mật</a> của Cunguoimoita.
                </p>
                <p class="register-link">
                    Bạn chưa có tài khoản?
                    <a href="${ctx}/register">Đăng ký</a>
                </p>
            </div>

            <%-- Khung 2: quên mật khẩu, nhập email --%>
            <div class="card-view" data-view="forgot-email" hidden>
                <h2>Quên mật khẩu</h2>
                <p class="card-hint">Nhập email tài khoản, chúng tôi sẽ gửi mã xác nhận gồm 6 số.</p>
                <p class="form-error" role="alert" data-error hidden></p>
                <form data-step="send">
                    <label class="sr-only" for="forgotEmail">Email</label>
                    <input id="forgotEmail" name="email" type="email" placeholder="Email" autocomplete="username" required>
                    <button class="submit" type="submit">GỬI MÃ XÁC NHẬN</button>
                </form>
                <p class="register-link"><button class="link-button" type="button" data-go="login">← Quay lại đăng nhập</button></p>
            </div>

            <%-- Khung 3: nhập mã --%>
            <div class="card-view" data-view="forgot-code" hidden>
                <h2>Nhập mã xác nhận</h2>
                <p class="card-hint">Mã đã gửi tới <strong data-reset-email></strong>.</p>
                <p class="form-success" role="status" data-note hidden></p>
                <p class="form-error" role="alert" data-error hidden></p>
                <form data-step="verify">
                    <label class="sr-only" for="resetCode">Mã xác nhận</label>
                    <input id="resetCode" class="code-input" name="code" type="text" inputmode="numeric"
                           pattern="[0-9 ]{6,7}" maxlength="7" placeholder="______" autocomplete="one-time-code" required>
                    <button class="submit" type="submit">XÁC NHẬN</button>
                </form>
                <p class="register-link">
                    Chưa nhận được mã?
                    <button class="link-button bold" type="button" data-resend>Gửi lại mã</button>
                    <br><button class="link-button" type="button" data-go="forgot-email">← Đổi email</button>
                </p>
            </div>

            <%-- Khung 4: mật khẩu mới --%>
            <div class="card-view" data-view="forgot-reset" hidden>
                <h2>Đặt mật khẩu mới</h2>
                <p class="form-error" role="alert" data-error hidden></p>
                <form data-step="reset">
                    <label class="sr-only" for="newPassword">Mật khẩu mới</label>
                    <input id="newPassword" name="password" type="password" placeholder="Mật khẩu mới"
                           autocomplete="new-password" data-pw-new required>
                    <label class="sr-only" for="confirmPassword">Nhập lại mật khẩu mới</label>
                    <input id="confirmPassword" name="confirmPassword" type="password" placeholder="Nhập lại mật khẩu mới"
                           autocomplete="new-password" data-pw-confirm="newPassword" required>
                    <button class="submit" type="submit">ĐỔI MẬT KHẨU</button>
                </form>
                <p class="register-link"><button class="link-button" type="button" data-go="login">← Quay lại đăng nhập</button></p>
            </div>
        </section>
    </div>
</main>

<%@ include file="/WEB-INF/views/common/auth-footer.jspf" %>

<script src="${ctx}/assets/js/password.js"></script>
<script src="${ctx}/assets/js/login.js"></script>
</body>
</html>
