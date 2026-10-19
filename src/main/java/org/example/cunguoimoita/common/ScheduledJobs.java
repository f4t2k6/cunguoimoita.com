package org.example.cunguoimoita.common;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.example.cunguoimoita.common.db.ConnectionPool;
import org.example.cunguoimoita.dispute.service.PayoutService;
import org.example.cunguoimoita.order.dao.VoucherDAO;
import org.example.cunguoimoita.order.service.OrderService;

import java.sql.Connection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Tác vụ chạy nền, bắt đầu khi Tomcat khởi động web và dừng khi tắt:
 *  1. F6: đơn đã giao quá 7 ngày, không khiếu nại → tự hoàn tất (kéo theo F8 xét hạng, F9 trả tiền người bán).
 *  2. F9: chuyển các khoản trả tiền đang chờ cho người bán vừa khai tài khoản nhận tiền.
 *  3. F4: voucher quá hạn chưa dùng → expired.
 * Chạy 2 phút sau khi khởi động, rồi mỗi giờ 1 lần. Admin bấm "Chạy ngay" ở trang Tổng quan cũng gọi runNow().
 */
@WebListener
public class ScheduledJobs implements ServletContextListener {

    private ScheduledExecutorService scheduler;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "cunguoimoita-jobs");
            t.setDaemon(true);           // không giữ Tomcat lại khi tắt
            return t;
        });
        scheduler.scheduleWithFixedDelay(ScheduledJobs::runQuietly, 2, 60, TimeUnit.MINUTES);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    /** Kết quả 1 lần chạy để báo cho admin. */
    public record Result(int completedOrders, int paidOutPayouts, int expiredVouchers) {
        public String summary() {
            return "Đã tự hoàn tất " + completedOrders + " đơn, chuyển " + paidOutPayouts
                    + " khoản tiền cho người bán, đánh dấu hết hạn " + expiredVouchers + " voucher.";
        }
    }

    /** Chạy cả 3 việc ngay. synchronized để lịch chạy nền và nút của admin không chạy chồng lên nhau. */
    public static synchronized Result runNow() throws Exception {
        int orders = new OrderService().autoCompleteDelivered();
        int payouts = new PayoutService().processScheduled();
        int vouchers;
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            vouchers = new VoucherDAO().expireOverdue(c);
        }
        return new Result(orders, payouts, vouchers);
    }

    // Lỗi trong 1 lần chạy (vd mất kết nối DB) chỉ ghi log, lần sau chạy lại; không được ném ra ngoài
    // vì ScheduledExecutorService sẽ dừng hẳn lịch nếu tác vụ ném lỗi.
    private static void runQuietly() {
        try {
            Result r = runNow();
            if (r.completedOrders() + r.paidOutPayouts() + r.expiredVouchers() > 0) {
                System.out.println("[ScheduledJobs] " + r.summary());
            }
        } catch (Exception e) {
            System.err.println("[ScheduledJobs] lỗi: " + e.getMessage());
        }
    }
}
