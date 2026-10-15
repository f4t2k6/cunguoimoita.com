-- Xác nhận email khi đăng ký (F1). Bảng phụ, không có trong class diagram.
-- Đăng ký xong chỉ lưu tạm ở đây và gửi mã 6 số về email; nhập đúng mã mới tạo dòng trong users/customers.
-- Đã gộp vào 00_database_chung.sql. Database chung (Neon) đã tạo trước đó thì chạy file này một lần.
CREATE TABLE IF NOT EXISTS email_verifications (
    email         VARCHAR(150) PRIMARY KEY,
    fullname      VARCHAR(100) NOT NULL,
    phone         VARCHAR(20),
    password_hash VARCHAR(255) NOT NULL,
    code_hash     CHAR(64)     NOT NULL,          -- SHA-256 của email + mã, không lưu mã gốc
    expires_at    TIMESTAMPTZ  NOT NULL,          -- mã hết hạn sau 10 phút
    attempts      INT          NOT NULL DEFAULT 0, -- số lần nhập sai (tối đa 5)
    last_sent_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP  -- chống gửi lại liên tục (60 giây)
);
