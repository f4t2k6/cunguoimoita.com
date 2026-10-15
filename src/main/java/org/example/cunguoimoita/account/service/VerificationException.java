package org.example.cunguoimoita.account.service;

/** Lỗi trong lúc đăng ký / xác nhận email, câu thông báo hiện thẳng cho người dùng. */
public class VerificationException extends RuntimeException {
    public VerificationException(String message) {
        super(message);
    }
}
