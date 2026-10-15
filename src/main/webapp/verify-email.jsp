<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Xác nhận email - Cunguoimoita</title>

    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/assets/css/register.css">
</head>
<body>

<header class="header">
    <div class="logo">Cunguoimoita</div>
    <div class="header-title">Xác nhận email</div>
    <a class="help" href="#">Bạn cần giúp đỡ?</a>
</header>

<main class="main">
    <div class="content">
        <section class="intro">
            <div class="intro-logo">Cunguoimoita</div>
            <h1 class="intro-title">
                <span>CŨ NGƯỜI</span>
                <span class="title-outline">MỚI TA</span>
            </h1>

            <p>
                Chợ đồ đã qua sử dụng cho sinh viên: đăng bán trong 1 phút,
                trả giá ngay trong tin nhắn, nhận hàng tận tay.
            </p>

            <div class="features">
                <span class="feature">◉ Trả giá qua chat</span>
                <span class="feature">✓ Thanh toán MoMo · VNPay</span>
                <span class="feature">★ Đánh giá người bán</span>
            </div>
        </section>

        <section class="form-card">
            <h2>Xác nhận email</h2>

            <p class="verify-text">
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

            <form action="${pageContext.request.contextPath}/register/verify" method="post">
                <input name="code"
                       class="code-input"
                       type="text"
                       inputmode="numeric"
                       pattern="[0-9 ]{6,7}"
                       maxlength="7"
                       autocomplete="one-time-code"
                       placeholder="Nhập mã 6 số"
                       aria-label="Mã xác nhận"
                       autofocus
                       required>
                <button class="submit" type="submit">XÁC NHẬN</button>
            </form>

            <form action="${pageContext.request.contextPath}/register/verify" method="post" class="resend-form">
                <input type="hidden" name="action" value="resend">
                Không nhận được mã?
                <button type="submit" class="link-button">Gửi lại mã</button>
            </form>

            <p class="signin">
                Nhập sai email?
                <a href="${pageContext.request.contextPath}/register">Đăng ký lại</a>
            </p>
        </section>
    </div>
</main>

<footer class="footer">
    <div class="footer-columns">
        <div>
            <h3>CHĂM SÓC KHÁCH HÀNG</h3>
            <a href="#">Trung tâm trợ giúp</a>
            <a href="#">Hướng dẫn mua hàng</a>
            <a href="#">Hướng dẫn bán hàng</a>
            <a href="#">Thanh toán</a>
            <a href="#">Liên hệ</a>
        </div>

        <div>
            <h3>VỀ CUNGUOIMOITA</h3>
            <a href="#">Giới thiệu</a>
            <a href="#">Điều khoản dịch vụ</a>
            <a href="#">Chính sách bảo mật</a>
            <a href="#">Hạng thành viên</a>
        </div>

        <div>
            <h3>THANH TOÁN</h3>
            <a href="#">MoMo　　VNPay</a>
        </div>

        <div>
            <h3>THEO DÕI CHÚNG TÔI</h3>
            <a href="#">Facebook</a>
            <a href="#">Instagram</a>
            <a href="#">TikTok</a>
        </div>
    </div>

    <div class="copyright">
        <span>© 2026 Cunguoimoita. Tất cả các quyền được bảo lưu.</span>
        <span>Chính sách bảo mật　 Quy chế hoạt động</span>
    </div>
</footer>

</body>
</html>
