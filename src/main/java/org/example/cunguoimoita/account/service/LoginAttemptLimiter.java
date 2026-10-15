package org.example.cunguoimoita.account.service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Chặn dò mật khẩu: đếm số lần đăng nhập sai của mỗi email trong một bảng băm (ConcurrentHashMap).
 * Sai {@value #MAX_FAILURES} lần liên tiếp thì khoá đăng nhập email đó {@value #LOCK_MINUTES} phút.
 * Bảng nằm trong bộ nhớ của Tomcat: tra cứu O(1), khởi động lại Tomcat thì xoá hết.
 */
public final class LoginAttemptLimiter {
    public static final int MAX_FAILURES = 5;
    public static final int LOCK_MINUTES = 5;
    private static final long LOCK_MS = LOCK_MINUTES * 60_000L;
    /** Lần sai cuối cách đây quá lâu thì đếm lại từ đầu. */
    private static final long RESET_MS = 15 * 60_000L;
    /** Giới hạn số email theo dõi để bảng không phình mãi. */
    private static final int MAX_ENTRIES = 10_000;

    /** Số lần sai và thời điểm; lockedUntil = 0 nghĩa là chưa bị khoá. */
    private record Attempt(int failures, long lastFailure, long lockedUntil) {
    }

    private static final LoginAttemptLimiter INSTANCE = new LoginAttemptLimiter();

    // key: email (chữ thường), value: số lần sai của email đó
    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();

    private LoginAttemptLimiter() {
    }

    public static LoginAttemptLimiter getInstance() {
        return INSTANCE;
    }

    /** Số giây còn bị khoá, 0 nếu được phép đăng nhập. */
    public long secondsLocked(String email) {
        Attempt a = attempts.get(email);
        if (a == null) {
            return 0;
        }
        long left = a.lockedUntil() - System.currentTimeMillis();
        return left > 0 ? (left + 999) / 1000 : 0;
    }

    /**
     * Ghi một lần đăng nhập sai.
     * @return số lần còn được thử trước khi bị khoá (0 nghĩa là vừa bị khoá)
     */
    public int recordFailure(String email) {
        long now = System.currentTimeMillis();
        if (attempts.size() >= MAX_ENTRIES) {
            attempts.values().removeIf(a -> a.lockedUntil() < now && now - a.lastFailure() > RESET_MS);
        }
        // compute() chạy nguyên tử trên từng key, hai request cùng lúc không đếm lệch
        Attempt a = attempts.compute(email, (k, old) -> {
            int failures = old == null || now - old.lastFailure() > RESET_MS || old.lockedUntil() != 0
                    ? 1 : old.failures() + 1;
            return new Attempt(failures, now, failures >= MAX_FAILURES ? now + LOCK_MS : 0);
        });
        return Math.max(0, MAX_FAILURES - a.failures());
    }

    /** Đăng nhập đúng thì xoá khỏi bảng. */
    public void recordSuccess(String email) {
        attempts.remove(email);
    }
}
