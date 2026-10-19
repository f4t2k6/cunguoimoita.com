<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Đăng ký - Cunguoimoita</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Bungee&family=Poppins:wght@300;400;500;600;700&display=swap" rel="stylesheet">
    <%-- Khung trang dùng chung với login.jsp (login.css); register.css chỉ thêm phần riêng của form đăng ký --%>
    <link rel="stylesheet" href="${ctx}/assets/css/login.css">
    <link rel="stylesheet" href="${ctx}/assets/css/register.css">
    <link rel="stylesheet" href="${ctx}/assets/css/password.css">
    <link rel="icon" type="image/png" href="${ctx}/assets/images/logo_small.png">
</head>
<body>
<header class="header">
    <a class="logo" href="${ctx}/" aria-label="Cunguoimoita">
        <img src="${ctx}/assets/images/logo.png" alt="Cunguoimoita">
    </a>
    <div class="header-title">Đăng ký</div>
</header>

<main class="main">
    <div class="content">
        <%@ include file="/WEB-INF/views/common/auth-intro.jspf" %>

        <section class="form-card">
            <h2>Đăng ký</h2>
            <c:if test="${not empty errorMessage}">
                <p class="form-error" role="alert"><c:out value="${errorMessage}"/></p>
            </c:if>
            <form action="${ctx}/register" method="post">
                <label class="sr-only" for="fullName">Họ và tên</label>
                <input id="fullName" name="fullName" type="text" value="<c:out value='${fullName}'/>"
                       placeholder="Họ và tên" autocomplete="name" required>
                <label class="sr-only" for="regEmail">Email</label>
                <input id="regEmail" name="email" type="email" value="<c:out value='${email}'/>"
                       placeholder="Email" autocomplete="email" required>
                <label class="sr-only" for="phone">Số điện thoại</label>
                <input id="phone" name="phone" type="tel" value="<c:out value='${phone}'/>"
                       placeholder="Số điện thoại" autocomplete="tel" required>
                <label class="sr-only" for="password">Mật khẩu</label>
                <input id="password" name="password" type="password" placeholder="Mật khẩu"
                       minlength="8" maxlength="128" autocomplete="new-password" data-pw-new required>
                <p class="hint">Ít nhất 8 ký tự, gồm chữ và số, không có khoảng trắng</p>
                <label class="sr-only" for="confirmPassword">Nhập lại mật khẩu</label>
                <input id="confirmPassword" name="confirmPassword" type="password" placeholder="Nhập lại mật khẩu"
                       maxlength="128" autocomplete="new-password" data-pw-confirm="password" required>
                <button class="submit" type="submit">ĐĂNG KÝ</button>
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
                Bằng việc đăng ký, bạn đồng ý với
                <a href="#">Điều khoản dịch vụ</a> và
                <a href="#">Chính sách bảo mật</a> của Cunguoimoita.
            </p>
            <p class="register-link">
                Bạn đã có tài khoản?
                <a href="${ctx}/login">Đăng nhập</a>
            </p>
        </section>
    </div>
</main>

<%@ include file="/WEB-INF/views/common/auth-footer.jspf" %>

<script src="${ctx}/assets/js/password.js"></script>
</body>
</html>
