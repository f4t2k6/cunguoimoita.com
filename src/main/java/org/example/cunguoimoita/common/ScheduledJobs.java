package org.example.cunguoimoita.common;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.example.cunguoimoita.dispute.service.PayoutService;
import org.example.cunguoimoita.order.service.OrderService;
import org.example.cunguoimoita.order.service.VoucherService;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Tác vụ nền mỗi giờ: tự hoàn tất đơn đã giao quá 7 ngày, chuyển tiền người bán đang chờ, đánh dấu voucher hết hạn.
 * Admin bấm "Chạy ngay" ở trang Tổng quan cũng gọi runNow().
 */
@WebListener
public class ScheduledJobs implements ServletContextListener {

    private ScheduledExecutorService scheduler;

    /** Kết quả 1 lần chạy để báo cho admin. */
    public record Result(int completedOrders, int paidOutPayouts, int expiredVouchers) {
        public String summary() {
            return "Đã tự hoàn tất " + completedOrders + " đơn, chuyển " + paidOutPayouts
                    + " khoản tiền cho người bán, đánh dấu hết hạn " + expiredVouchers + " voucher.";
        }
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "cunguoimoita-jobs");
            t.setDaemon(true);
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

    /** Chạy cả 3 việc ngay. synchronized để lịch chạy nền và nút của admin không chạy chồng lên nhau. */
    public static synchronized Result runNow() throws Exception {
        int orders = new OrderService().autoCompleteDelivered();
        int payouts = new PayoutService().processScheduled();
        int vouchers = new VoucherService().expireOverdue();
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
