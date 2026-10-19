# Cấu hình đăng nhập Google và Facebook

## 1. Chạy migration trên Neon

Chọn đúng Neon project, branch và database mà Tomcat đang dùng, sau đó chạy:

```sql
-- nội dung src/main/resources/sql/11_social_accounts.sql
```

Migration tạo bảng `social_accounts`, dùng để liên kết ID OAuth của Google/Facebook với customer trong `users`.

## 2. Lấy thông tin OAuth

### Google

Trong Google Cloud Console, tạo OAuth Client loại **Web application**. Thêm callback chính xác của ứng dụng đang chạy vào **Authorized redirect URIs**:

```text
http://localhost:8080/<context-path>/oauth/google/callback
```

Nếu project được deploy ở root, bỏ `<context-path>` và dấu `/` tương ứng. Google yêu cầu redirect URI phải khớp chính xác với URI đã khai báo.

### Facebook

Trong Meta for Developers, tạo app có Facebook Login, thêm callback chính xác vào **Valid OAuth Redirect URIs**:

```text
http://localhost:8080/<context-path>/oauth/facebook/callback
```

Khi app ở Development mode, tài khoản Facebook kiểm thử cần có vai trò trong app (admin/developer/tester). Quyền email được yêu cầu để ứng dụng tạo hồ sơ customer.

## 3. Đặt environment variables riêng trong IntelliJ

Mở **Run → Edit Configurations… → cấu hình Tomcat → Environment variables** và thêm các biến sau cho cấu hình local, không chia sẻ/commit cấu hình này:

```text
GOOGLE_CLIENT_ID=<Google OAuth Client ID>
GOOGLE_CLIENT_SECRET=<Google OAuth Client Secret>
GOOGLE_REDIRECT_URI=http://localhost:8080/<context-path>/oauth/google/callback

FACEBOOK_APP_ID=<Meta App ID>
FACEBOOK_APP_SECRET=<Meta App Secret>
FACEBOOK_REDIRECT_URI=http://localhost:8080/<context-path>/oauth/facebook/callback
FACEBOOK_GRAPH_VERSION=v26.0
```

Chỉ nhập các secret vào cấu hình chạy local của IDE. Không đặt chúng trong JSP, Java, `context.xml`, file `.env` được commit hoặc gửi qua chat.

## 4. Chạy lại

Lưu cấu hình, khởi động lại Tomcat, sau đó thử lần lượt nút Google và Facebook. Lần đăng nhập đầu tiên tạo customer; các lần sau dùng lại liên kết `social_accounts`. Google chỉ được liên kết tự động với tài khoản customer hiện có khi Google xác nhận email. Facebook không tự liên kết email trùng với tài khoản mật khẩu hiện hữu; hãy đăng nhập tài khoản đó bằng mật khẩu trước.
