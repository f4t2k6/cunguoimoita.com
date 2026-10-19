<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Đăng nhập - Cunguoimoita</title>
    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/assets/css/login.css">
</head>
<body>
<header class="header">
    <a class="logo" href="${pageContext.request.contextPath}/">Cunguoimoita</a>
    <div class="header-title">Đăng nhập</div>
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
            <h2>Đăng nhập</h2>
            <% if ("1".equals(request.getParameter("registered"))) { %>
                <p class="form-success" role="status">Đăng ký thành công. Hãy đăng nhập.</p>
            <% } %>
            <% if ("1".equals(request.getParameter("banned"))) { %>
                <p class="form-error" role="alert">Tài khoản này đã bị khóa.</p>
            <% } %>
            <% if ("1".equals(request.getParameter("loggedOut"))) { %>
                <p class="form-success" role="status">Bạn đã đăng xuất.</p>
            <% } %>
            <% String socialError = request.getParameter("socialError");
               if ("not_configured".equals(socialError)) { %>
                <p class="form-error" role="alert">Đăng nhập mạng xã hội chưa được cấu hình trên máy chủ.</p>
            <% } else if ("oauth_cancelled".equals(socialError)) { %>
                <p class="form-error" role="alert">Bạn đã hủy đăng nhập bằng mạng xã hội.</p>
            <% } else if ("email_exists".equals(socialError)) { %>
                <p class="form-error" role="alert">Email này đã có tài khoản. Hãy đăng nhập bằng mật khẩu trước.</p>
            <% } else if ("banned".equals(socialError)) { %>
                <p class="form-error" role="alert">Tài khoản của bạn đang bị khóa.</p>
            <% } else if ("oauth_failed".equals(socialError)) { %>
                <p class="form-error" role="alert">Không thể xác thực tài khoản mạng xã hội. Vui lòng thử lại.</p>
            <% } %>
            <% if (request.getAttribute("errorMessage") != null) { %>
                <p class="form-error" role="alert">
                    <%= request.getAttribute("errorMessage") %>
                </p>
            <% } %>
            <form action="${pageContext.request.contextPath}/login" method="post">
                <label class="sr-only" for="email">Email</label>
                <input id="email"
                       name="email"
                       type="email"
                       placeholder="Email"
                       autocomplete="username"
                       required>
                <label class="sr-only" for="password">Mật khẩu</label>
                <input id="password"
                       name="password"
                       type="password"
                       placeholder="Mật khẩu"
                       autocomplete="current-password"
                       required>
                <div class="form-options">
                    <a class="forgot-link" href="#">Quên mật khẩu?</a>
                </div>
                <button class="submit" type="submit">ĐĂNG NHẬP</button>
            </form>
            <div class="divider">HOẶC</div>
            <div class="social">
                <form action="${pageContext.request.contextPath}/oauth/facebook/start" method="get">
                    <input type="hidden" name="from" value="login">
                    <button type="submit">🔵 Facebook</button>
                </form>
                <form action="${pageContext.request.contextPath}/oauth/google/start" method="get">
                    <input type="hidden" name="from" value="login">
                    <button type="submit">🌐 Google</button>
                </form>
            </div>
            <p class="terms">
                Bằng việc đăng nhập, bạn đồng ý với
                <a href="#">Điều khoản dịch vụ</a> và
                <a href="#">Chính sách bảo mật</a> của Cunguoimoita.
            </p>
            <p class="register-link">
                Bạn chưa có tài khoản?
                <a href="${pageContext.request.contextPath}/register.jsp">Đăng ký</a>
            </p>
        </section>
    </div>
</main>
<footer class="footer">
