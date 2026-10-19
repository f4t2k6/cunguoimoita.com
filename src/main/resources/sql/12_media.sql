-- Ảnh dùng chung cho cả web: ảnh đại diện, ảnh bài đăng, ảnh chat, ảnh khiếu nại...
-- Lưu nội dung ảnh trong database để mọi máy chạy Tomcat đều đọc được.
CREATE TABLE IF NOT EXISTS media (
                                     id           VARCHAR(36)  PRIMARY KEY,
    owner_id     VARCHAR(36)  REFERENCES users(id) ON DELETE SET NULL,  -- ai tải lên
    content_type VARCHAR(50)  NOT NULL,                                 -- image/jpeg
    data         BYTEA        NOT NULL,                                 -- nội dung ảnh
    size_bytes   INTEGER      NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
    );