package org.example.cunguoimoita.payment;

import org.example.cunguoimoita.payment.model.Payment;
import org.example.cunguoimoita.payment.model.PaymentGateway;
import org.example.cunguoimoita.payment.model.PaymentStatus;
import org.example.cunguoimoita.payment.service.VNPayConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử Phân hệ F5: Thanh toán Escrow & VNPay Signature")
class PaymentTest {

    @Test
    @DisplayName("Khởi tạo Payment mặc định ở trạng thái PENDING")
    void testPaymentInitialization() {
        Payment p = new Payment();
        p.setId("PAY-01");
        p.setAmount(1330000);
        p.setGateway(PaymentGateway.VNPAY);

        assertEquals(PaymentStatus.PENDING, p.getStatus());
        assertNotNull(p.getCreatedAt());
        assertNull(p.getPaidAt());
        assertNull(p.getGatewayTxnId());
    }

    @Test
    @DisplayName("Payment.markPaid() chuyển trạng thái sang PAID và lưu vết giao dịch")
    void testMarkPaid() {
        Payment p = new Payment();
        p.markPaid("VNP14289903");

        assertEquals(PaymentStatus.PAID, p.getStatus());
        assertEquals("VNP14289903", p.getGatewayTxnId());
        assertNotNull(p.getPaidAt());
    }

    @Test
    @DisplayName("Payment.markFailed() chuyển trạng thái sang FAILED")
    void testMarkFailed() {
        Payment p = new Payment();
        p.markFailed();

        assertEquals(PaymentStatus.FAILED, p.getStatus());
    }

    @Test
    @DisplayName("Payment.refund() chuyển trạng thái sang REFUNDED")
    void testRefund() {
        Payment p = new Payment();
        p.markPaid("TXN-123");
        p.refund();

        assertEquals(PaymentStatus.REFUNDED, p.getStatus());
    }

    @Test
    @DisplayName("VNPayConfig sinh URL thanh toán chứa đủ tham số vnp_Command, vnp_Amount, vnp_SecureHash")
    void testVNPayUrlGeneration() {
        String paymentUrl = VNPayConfig.buildPaymentUrl("ORD-001", 1330000, "Thanh toan don hang ORD-001", "127.0.0.1", "http://localhost:8080/cunguoimoita/payment/vnpay-return");

        assertNotNull(paymentUrl);
        assertTrue(paymentUrl.startsWith("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?"));
        assertTrue(paymentUrl.contains("vnp_Amount=133000000")); // amount * 100
        assertTrue(paymentUrl.contains("vnp_Command=pay"));
        assertTrue(paymentUrl.contains("vnp_SecureHash="));
    }

    @Test
    @DisplayName("VNPayConfig xác thực chữ ký số HMAC-SHA512 chính xác")
    void testVNPaySignatureVerification() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Amount", "133000000");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", VNPayConfig.getTmnCode());
        params.put("vnp_TxnRef", "ORD-001");

        String hashData = "vnp_Amount=133000000&vnp_Command=pay&vnp_TmnCode=" + VNPayConfig.getTmnCode() + "&vnp_TxnRef=ORD-001";
        String expectedHash = VNPayConfig.hmacSHA512(VNPayConfig.getHashSecret(), hashData);

        assertTrue(VNPayConfig.verifySignature(params, expectedHash));
        assertFalse(VNPayConfig.verifySignature(params, "fake_hash_123456"));
    }
}
