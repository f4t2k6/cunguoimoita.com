<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Xác nhận email - Cunguoimoita</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Bungee&family=Poppins:wght@300;400;500;600;700&display=swap" rel="stylesheet">
    <%-- Khung trang dùng chung với login.jsp (login.css); register.css chỉ thêm phần riêng của form đăng ký --%>
    <link rel="stylesheet" href="${ctx}/assets/css/login.css">
    <link rel="stylesheet" href="${ctx}/assets/css/register.css">
    <link rel="icon" type="image/png" href="${ctx}/assets/images/logo_small.png">
</head>
<body>
<header class="header">
    <a class="logo" href="${ctx}/" aria-label="Cunguoimoita">
        <img src="${ctx}/assets/images/logo.png" alt="Cunguoimoita">
    </a>
    <div class="header-title">Xác nhận email</div>
</header>

<main class="main">
    <div class="content">
        <%@ include file="/WEB-INF/views/common/auth-intro.jspf" %>

        <section class="form-card">
            <h2>Xác nhận email</h2>
            <p class="card-hint">
                Mã gồm 6 chữ số đã được gửi tới <strong><c:out value="${email}"/></strong>.
                Nhập mã để hoàn tất đăng ký. Mã có hiệu lực trong ${validMinutes} phút.
            </p>
            <c:if test="${not mailConfigured}">
                <p class="hint verify-dev">
                    Máy này chưa cấu hình tài khoản gửi email (mail.username, mail.password trong context.xml),
                    nên mã được in ra cửa sổ log của Tomcat.
                </p>
            </c:if>
            <c:if test="${not empty successMessage}">
                <p class="form-success" role="status"><c:out value="${successMessage}"/></p>
            </c:if>
            <c:if test="${not empty errorMessage}">
                <p class="form-error" role="alert"><c:out value="${errorMessage}"/></p>
            </c:if>
            <form action="${ctx}/register/verify" method="post">
                <input name="code" class="code-input" type="text" inputmode="numeric" pattern="[0-9 ]{6,7}"
                       maxlength="7" autocomplete="one-time-code" placeholder="______" aria-label="Mã xác nhận"
                       autofocus required>
                <button class="submit" type="submit">XÁC NHẬN</button>
            </form>
            <form action="${ctx}/register/verify" method="post" class="resend-form">
                <input type="hidden" name="action" value="resend">
                Chưa nhận được mã?
                <button type="submit" class="link-button bold">Gửi lại mã</button>
            </form>
            <p class="register-link">
                Nhập sai email?
                <a href="${ctx}/register">Đăng ký lại</a>
            </p>
        </section>
    </div>
</main>

<%@ include file="/WEB-INF/views/common/auth-footer.jspf" %>
</body>
</html>
