package org.example.cunguoimoita.shipping.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Class Shipment trong class diagram. Chỉ chứa luật nghiệp vụ, việc lưu DB do ShipmentDAO làm. */
public class Shipment {

    private String id;
    private String orderId;
    private String carrierId;
    private String trackingCode;
    private double shippingFee;
    private ShipmentStatus status = ShipmentStatus.CREATED;
    private LocalDate createdDate;
    private LocalDate deliveredDate;
    private double codAmount;                    // 0 nếu đơn đã trả trước online
    private CodStatus codStatus = CodStatus.NONE;
    private LocalDateTime codCollectedAt;        // shipper thu tiền của người mua
    private LocalDateTime codRemittedAt;         // shipper nộp tiền về sàn

    /** + updateStatus(newStatus): void */
    public void updateStatus(ShipmentStatus newStatus) {
        if (!status.canMoveTo(newStatus)) {
            throw new IllegalStateException("Không thể chuyển vận đơn từ " + status.dbValue() + " sang " + newStatus.dbValue());
        }
        if (newStatus == ShipmentStatus.DELIVERED && codStatus == CodStatus.PENDING) {
            throw new IllegalStateException("Đơn COD phải ghi nhận shipper đã thu tiền trước khi báo đã giao");
        }
        status = newStatus;
        if (newStatus == ShipmentStatus.DELIVERED) {
            deliveredDate = LocalDate.now();
        }
    }

    /** + track(): String */
    public String track() {
        String code = trackingCode == null ? "(chưa có mã vận đơn)" : trackingCode;
        return code + " - " + status.label();
    }

    /** + confirmCodCollected(): void */
    public void confirmCodCollected() {
        if (codStatus != CodStatus.PENDING) {
            throw new IllegalStateException("Vận đơn không ở trạng thái chờ thu tiền COD");
        }
        codStatus = CodStatus.COLLECTED;
        codCollectedAt = LocalDateTime.now();
    }

    /** + confirmCodRemitted(): void */
    public void confirmCodRemitted() {
        if (codStatus != CodStatus.COLLECTED) {
            throw new IllegalStateException("Shipper chưa thu tiền COD nên chưa thể nộp về sàn");
        }
        codStatus = CodStatus.REMITTED;
        codRemittedAt = LocalDateTime.now();
    }

    public boolean isCod() {
        return codStatus != CodStatus.NONE;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getCarrierId() { return carrierId; }
    public void setCarrierId(String carrierId) { this.carrierId = carrierId; }
    public String getTrackingCode() { return trackingCode; }
    public void setTrackingCode(String trackingCode) { this.trackingCode = trackingCode; }
    public double getShippingFee() { return shippingFee; }
    public void setShippingFee(double shippingFee) { this.shippingFee = shippingFee; }
    public ShipmentStatus getStatus() { return status; }
    public void setStatus(ShipmentStatus status) { this.status = status; }
    public LocalDate getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDate createdDate) { this.createdDate = createdDate; }
    public LocalDate getDeliveredDate() { return deliveredDate; }
    public void setDeliveredDate(LocalDate deliveredDate) { this.deliveredDate = deliveredDate; }
    public double getCodAmount() { return codAmount; }
    public void setCodAmount(double codAmount) { this.codAmount = codAmount; }
    public CodStatus getCodStatus() { return codStatus; }
    public void setCodStatus(CodStatus codStatus) { this.codStatus = codStatus; }
    public LocalDateTime getCodCollectedAt() { return codCollectedAt; }
    public void setCodCollectedAt(LocalDateTime codCollectedAt) { this.codCollectedAt = codCollectedAt; }
    public LocalDateTime getCodRemittedAt() { return codRemittedAt; }
    public void setCodRemittedAt(LocalDateTime codRemittedAt) { this.codRemittedAt = codRemittedAt; }
}
