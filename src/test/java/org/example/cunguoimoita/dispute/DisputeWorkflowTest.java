package org.example.cunguoimoita.dispute;

import org.example.cunguoimoita.dispute.model.Dispute;
import org.example.cunguoimoita.dispute.model.DisputeReason;
import org.example.cunguoimoita.dispute.model.DisputeStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử Phân hệ F9: Vòng đời xử lý tranh chấp Dispute")
class DisputeWorkflowTest {

    @Test
    @DisplayName("Khởi tạo Dispute mặc định ở trạng thái OPEN")
    void testDisputeInitialization() {
        Dispute d = new Dispute();
        d.setId("DSP-01");
        d.setOrderId("ORD-001");
        d.setReason(DisputeReason.DAMAGED);

        assertEquals(DisputeStatus.OPEN, d.getStatus());
        assertNotNull(d.getCreatedDate());
        assertNull(d.getResolvedDate());
        assertNull(d.getHandledByAdminId());
    }

    @Test
    @DisplayName("submitEvidence() nối thêm ảnh bằng chứng mới")
    void testSubmitEvidence() {
        Dispute d = new Dispute();
        d.setEvidenceImages("photo_01.jpg");
        d.submitEvidence("photo_02.jpg");

        assertEquals("photo_01.jpg,photo_02.jpg", d.getEvidenceImages());
    }

    @Test
    @DisplayName("resolveForBuyer() chuyển trạng thái sang RESOLVED_REFUND và lưu adminId")
    void testResolveForBuyer() {
        Dispute d = new Dispute();
        d.resolveForBuyer("A1", "Đồng ý hoàn tiền do lỗi vận chuyển bể switch");

        assertEquals(DisputeStatus.RESOLVED_REFUND, d.getStatus());
        assertEquals("A1", d.getHandledByAdminId());
        assertEquals("Đồng ý hoàn tiền do lỗi vận chuyển bể switch", d.getResolutionNote());
        assertNotNull(d.getResolvedDate());
    }

    @Test
    @DisplayName("resolveForSeller() chuyển trạng thái sang RESOLVED_PAYOUT")
    void testResolveForSeller() {
        Dispute d = new Dispute();
        d.resolveForSeller("A1", "Bác khiếu nại, người mua tự làm vỡ");

        assertEquals(DisputeStatus.RESOLVED_PAYOUT, d.getStatus());
        assertEquals("A1", d.getHandledByAdminId());
        assertNotNull(d.getResolvedDate());
    }

    @Test
    @DisplayName("reject() chuyển trạng thái sang REJECTED")
    void testReject() {
        Dispute d = new Dispute();
        d.reject("A1", "Không đủ chứng cứ");

        assertEquals(DisputeStatus.REJECTED, d.getStatus());
    }
}
