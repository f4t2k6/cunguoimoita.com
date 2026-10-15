package org.example.cunguoimoita.dispute;

import org.example.cunguoimoita.dispute.model.Payout;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử Phân hệ F9: Công thức tính giải ngân Payout.calculateNetAmount()")
class PayoutCalculationTest {

    @Test
    @DisplayName("Hạng Bronze (Phí sàn 5%): Đơn 2.000.000 đ trừ phí 100.000 đ, thực nhận 1.900.000 đ")
    void testBronzeTierPayout() {
        double gross = 2000000;
        double rate = 0.05; // 5%
        double net = Payout.calculateNetAmount(gross, rate);

        assertEquals(1900000.0, net);
    }

    @Test
    @DisplayName("Hạng Silver (Phí sàn 4%): Đơn 2.000.000 đ trừ phí 80.000 đ, thực nhận 1.920.000 đ")
    void testSilverTierPayout() {
        double gross = 2000000;
        double rate = 0.04; // 4%
        double net = Payout.calculateNetAmount(gross, rate);

        assertEquals(1920000.0, net);
    }

    @Test
    @DisplayName("Hạng Gold (Phí sàn 3%): Đơn 2.000.000 đ trừ phí 60.000 đ, thực nhận 1.940.000 đ")
    void testGoldTierPayout() {
        double gross = 2000000;
        double rate = 0.03; // 3%
        double net = Payout.calculateNetAmount(gross, rate);

        assertEquals(1940000.0, net);
    }

    @Test
    @DisplayName("Hạng Diamond (Phí sàn 1.5%): Đơn 2.000.000 đ trừ phí 30.000 đ, thực nhận 1.970.000 đ")
    void testDiamondTierPayout() {
        double gross = 2000000;
        double rate = 0.015; // 1.5%
        double net = Payout.calculateNetAmount(gross, rate);

        assertEquals(1970000.0, net);
    }

    @Test
    @DisplayName("Đơn hàng giá trị 0 đ cho kết quả thực nhận 0 đ")
    void testZeroGrossPayout() {
        double net = Payout.calculateNetAmount(0, 0.05);
        assertEquals(0.0, net);
    }
}
