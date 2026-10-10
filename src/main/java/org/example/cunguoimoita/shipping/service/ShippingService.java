package org.example.cunguoimoita.shipping.service;

import org.example.cunguoimoita.shipping.dao.ShipmentDAO;
import org.example.cunguoimoita.shipping.dao.ShippingCarrierDAO;
import org.example.cunguoimoita.shipping.model.CodStatus;
import org.example.cunguoimoita.shipping.model.Shipment;
import org.example.cunguoimoita.shipping.model.ShipmentStatus;
import org.example.cunguoimoita.shipping.model.ShippingCarrier;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Nghiệp vụ F6. Các chức năng khác gọi vào đây, không gọi thẳng DAO:
 *  - F4 (TV3) trang thanh toán: listCarriers(), calculateFee(...)
 *  - F4 (TV3) Order.chooseCarrier(carrierId): createShipment(...)
 */
public class ShippingService {

    private final ShippingCarrierDAO carrierDAO = new ShippingCarrierDAO();
    private final ShipmentDAO shipmentDAO = new ShipmentDAO();

    public List<ShippingCarrier> listCarriers() throws SQLException {
        return carrierDAO.findActive();
    }

    public double calculateFee(String carrierId, String fromCity, String toCity) throws SQLException {
        return requireCarrier(carrierId).calculateFee(fromCity, toCity);
    }

    /**
     * Tạo vận đơn khi người mua chọn đơn vị vận chuyển.
     * @param codAmount số tiền shipper phải thu; 0 nếu đơn trả trước online
     */
    public Shipment createShipment(String orderId, String carrierId, String fromCity, String toCity,
                                   double codAmount) throws SQLException {
        ShippingCarrier carrier = requireCarrier(carrierId);
        Shipment s = new Shipment();
        s.setId(UUID.randomUUID().toString());
        s.setOrderId(orderId);
        s.setCarrierId(carrierId);
        s.setShippingFee(carrier.calculateFee(fromCity, toCity));
        s.setCreatedDate(LocalDate.now());
        s.setCodAmount(codAmount);
        s.setCodStatus(codAmount > 0 ? CodStatus.PENDING : CodStatus.NONE);
        shipmentDAO.insert(s);
        return s;
    }

    public Shipment findByOrder(String orderId) throws SQLException {
        return shipmentDAO.findByOrderId(orderId);
    }

    /** Người bán bấm "Đã gửi hàng" và nhập mã vận đơn. */
    public void markShipped(String orderId, String trackingCode) throws SQLException {
        Shipment s = requireShipment(orderId);
        s.setTrackingCode(trackingCode);
        s.updateStatus(ShipmentStatus.PICKED_UP);
        shipmentDAO.update(s);
        // TODO: gọi OrderService.confirmShipped(orderId) của TV3 khi có: đơn paid → shipping
    }

    /** Nút giả lập cập nhật trạng thái (không nối API GHN/GHTK thật). */
    public void updateStatus(String orderId, ShipmentStatus newStatus) throws SQLException {
        Shipment s = requireShipment(orderId);
        s.updateStatus(newStatus);
        shipmentDAO.update(s);
        // TODO: khi DELIVERED, báo OrderService của TV3 để đơn chuyển delivered
        // TODO: gửi thông báo qua NotificationService.send(...) (F7)
    }

    public void confirmCodCollected(String orderId) throws SQLException {
        Shipment s = requireShipment(orderId);
        s.confirmCodCollected();
        shipmentDAO.update(s);
    }

    public void confirmCodRemitted(String orderId) throws SQLException {
        Shipment s = requireShipment(orderId);
        s.confirmCodRemitted();
        shipmentDAO.update(s);
    }

    private ShippingCarrier requireCarrier(String carrierId) throws SQLException {
        ShippingCarrier carrier = carrierDAO.findById(carrierId);
        if (carrier == null || !carrier.isActive()) {
            throw new IllegalArgumentException("Đơn vị vận chuyển không tồn tại hoặc đã ngừng: " + carrierId);
        }
        return carrier;
    }

    private Shipment requireShipment(String orderId) throws SQLException {
        Shipment s = shipmentDAO.findByOrderId(orderId);
        if (s == null) {
            throw new IllegalArgumentException("Đơn " + orderId + " chưa có vận đơn");
        }
        return s;
    }
}
