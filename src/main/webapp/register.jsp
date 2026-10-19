<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Đăng ký - Cunguoimoita</title>

    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/assets/css/register.css">
</head>
<body>

<header class="header">
    <div class="logo">Cunguoimoita</div>
    <div class="header-title">Đăng ký</div>
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
            <h2>Đăng ký</h2>

            <% if (request.getAttribute("errorMessage") != null) { %>
                <p class="form-error" role="alert">
                    <%= request.getAttribute("errorMessage") %>
                </p>
            <% } %>
            <% String socialError = request.getParameter("socialError");
               if ("not_configured".equals(socialError)) { %>
                <p class="form-error" role="alert">Đăng ký mạng xã hội chưa được cấu hình trên máy chủ.</p>
            <% } else if ("oauth_cancelled".equals(socialError)) { %>
                <p class="form-error" role="alert">Bạn đã hủy đăng ký bằng mạng xã hội.</p>
            <% } else if ("email_exists".equals(socialError)) { %>
                <p class="form-error" role="alert">Email này đã có tài khoản. Hãy đăng nhập bằng mật khẩu trước.</p>
            <% } else if ("banned".equals(socialError)) { %>
                <p class="form-error" role="alert">Tài khoản của bạn đang bị khóa.</p>
            <% } else if ("oauth_failed".equals(socialError)) { %>
                <p class="form-error" role="alert">Không thể xác thực tài khoản mạng xã hội. Vui lòng thử lại.</p>
            <% } %>

            <form action="${pageContext.request.contextPath}/register"
                  method="post">
                <input name="fullName"
                       type="text"
                       placeholder="Họ và tên"
                       required>

                <input name="email"
                       type="email"
                       placeholder="Email"
                       required>

                <input name="phone"
                       type="tel"
                       placeholder="Số điện thoại"
                       required>

                <input name="password"
                       type="password"
                       placeholder="Mật khẩu"
                       minlength="8"
                       required>

                <p class="hint">Ít nhất 8 ký tự, gồm chữ và số</p>

                <input name="confirmPassword"
                       type="password"
                       placeholder="Nhập lại mật khẩu"
                       minlength="8"
                       required>

                <button class="submit" type="submit">ĐĂNG KÝ</button>
            </form>

            <div class="divider">HOẶC</div>

            <div class="social">
                <form action="${pageContext.request.contextPath}/oauth/facebook/start" method="get">
                    <input type="hidden" name="from" value="register">
                    <button type="submit">🔵 Facebook</button>
                </form>
                <form action="${pageContext.request.contextPath}/oauth/google/start" method="get">
                    <input type="hidden" name="from" value="register">
                    <button type="submit">🌐 Google</button>
                </form>
            </div>

            <p class="terms">
                Bằng việc đăng ký, bạn đồng ý với
                <a href="#">Điều khoản dịch vụ</a> và
                <a href="#">Chính sách bảo mật</a> của Cunguoimoita.
            </p>

            <p class="signin">
                Bạn đã có tài khoản?
                <a href="${pageContext.request.contextPath}/login">Đăng nhập</a>
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
