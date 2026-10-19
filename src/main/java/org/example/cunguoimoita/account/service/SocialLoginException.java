package org.example.cunguoimoita.account.service;

/** Lỗi đăng nhập xã hội có thể giải thích an toàn cho người dùng. */
public class SocialLoginException extends Exception {
    public SocialLoginException(String message) {
        super(message);
    }
}
