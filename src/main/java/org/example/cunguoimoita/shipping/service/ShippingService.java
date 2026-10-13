package org.example.cunguoimoita.shipping.service;

import org.example.cunguoimoita.common.dao.OrderRefDAO;
import org.example.cunguoimoita.common.dao.OrderRefDAO.OrderRef;
import org.example.cunguoimoita.common.db.ConnectionPool;
import org.example.cunguoimoita.notification.model.NotificationType;
import org.example.cunguoimoita.notification.service.NotificationService;
import org.example.cunguoimoita.shipping.dao.ShipmentDAO;
import org.example.cunguoimoita.shipping.dao.ShippingCarrierDAO;
import org.example.cunguoimoita.shipping.model.CodStatus;
import org.example.cunguoimoita.shipping.model.Shipment;
import org.example.cunguoimoita.shipping.model.ShipmentStatus;
import org.example.cunguoimoita.shipping.model.ShippingCarrier;

import java.sql.Connection;
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
    private final OrderRefDAO orderDAO = new OrderRefDAO();
    private final NotificationService notificationService = new NotificationService();

    public List<ShippingCarrier> listCarriers() throws SQLException {
        return carrierDAO.findActive();
    }

    public double calculateFee(String carrierId, String fromCity, String toCity) throws SQLException {
        return requireCarrier(carrierId).calculateFee(fromCity, toCity);
    }

    /**
     * Order.chooseCarrier(carrierId): Shipment — tạo vận đơn và ghi phí ship vào đơn.
     * fromCity/toCity để trống thì lấy theo địa chỉ mặc định người bán và địa chỉ giao của đơn.
     * @param codAmount số tiền shipper phải thu; 0 nếu đơn trả trước online
     */
    public Shipment createShipment(String orderId, String carrierId, String fromCity, String toCity,
                                   double codAmount) throws SQLException {
        ShippingCarrier carrier = requireCarrier(carrierId);
        OrderRef order = requireOrder(orderId);
        if (shipmentDAO.findByOrderId(orderId) != null) {
            throw new IllegalArgumentException("Đơn " + order.code() + " đã có vận đơn");
        }
        String from = isBlank(fromCity) ? order.fromCity() : fromCity;
        String to = isBlank(toCity) ? order.toCity() : toCity;
        Shipment s = new Shipment();
        s.setId(UUID.randomUUID().toString());
        s.setOrderId(orderId);
        s.setCarrierId(carrierId);
        s.setShippingFee(carrier.calculateFee(from, to));
        s.setCreatedDate(LocalDate.now());
        s.setCodAmount(codAmount);
        s.setCodStatus(codAmount > 0 ? CodStatus.PENDING : CodStatus.NONE);
        shipmentDAO.insert(s);
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            orderDAO.updateShippingFee(c, orderId, s.getShippingFee());
        }
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
        syncOrder(s);
    }

    /** Nút giả lập cập nhật trạng thái (không nối API GHN/GHTK thật). */
    public void updateStatus(String orderId, ShipmentStatus newStatus) throws SQLException {
        Shipment s = requireShipment(orderId);
        s.updateStatus(newStatus);
        shipmentDAO.update(s);
        syncOrder(s);
    }

    /**
     * Cập nhật trạng thái Order theo vận đơn rồi báo người mua:
     * picked_up / in_transit → shipping (Order.confirmShipped), delivered → delivered.
     * returned chưa đổi đơn: TV4 xử lý hoàn tiền / khiếu nại.
     * TODO: khi TV3 có OrderService thì gọi confirmShipped()/... của TV3 thay cho OrderRefDAO.
     */
    private void syncOrder(Shipment s) throws SQLException {
        OrderRef order;
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            order = orderDAO.find(c, s.getOrderId());
            if (order == null) {
                return;
            }
            switch (s.getStatus()) {
                case PICKED_UP:
                case IN_TRANSIT:
                    orderDAO.updateStatus(c, order.id(), "shipping", "new", "awaiting_payment", "paid");
                    break;
                case DELIVERED:
                    orderDAO.updateStatus(c, order.id(), "delivered", "new", "awaiting_payment", "paid", "shipping");
                    break;
                default:
                    break;
            }
        }
        notificationService.send(order.buyerId(), NotificationType.SHIPMENT,
                "Đơn " + order.code() + ": " + s.getStatus().label(),
                "/shipping/track?orderId=" + order.id());
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

    private OrderRef requireOrder(String orderId) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            OrderRef order = orderId == null ? null : orderDAO.find(c, orderId);
            if (order == null) {
                throw new IllegalArgumentException("Không tìm thấy đơn hàng " + orderId);
            }
            return order;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private Shipment requireShipment(String orderId) throws SQLException {
        Shipment s = shipmentDAO.findByOrderId(orderId);
        if (s == null) {
            throw new IllegalArgumentException("Đơn " + orderId + " chưa có vận đơn");
        }
        return s;
    }
}
