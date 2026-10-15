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
import java.util.function.Consumer;

/**
 * Nghiệp vụ F6. Các chức năng khác gọi vào đây, không gọi thẳng DAO:
 *  - F4 (TV3) trang thanh toán: listCarriers(), calculateFee(...)
 *  - F4 (TV3) Order.chooseCarrier(carrierId): createShipment(...)
 * Mỗi thao tác dùng 1 kết nối (1 transaction) cho mọi câu lệnh, thông báo gửi sau khi commit.
 */
public class ShippingService {

    /** Danh sách hãng vận chuyển rất ít đổi nên giữ trong bộ nhớ 5 phút, khỏi hỏi DB mỗi lần tính phí. */
    private static final long CARRIER_CACHE_MS = 5 * 60 * 1000;
    private static volatile List<ShippingCarrier> carrierCache;
    private static volatile long carrierCacheAt;

    private final ShippingCarrierDAO carrierDAO = new ShippingCarrierDAO();
    private final ShipmentDAO shipmentDAO = new ShipmentDAO();
    private final OrderRefDAO orderDAO = new OrderRefDAO();
    private final NotificationService notificationService = new NotificationService();

    /** Các hãng đang hoạt động, rẻ trước. */
    public List<ShippingCarrier> listCarriers() throws SQLException {
        List<ShippingCarrier> cached = carrierCache;
        if (cached == null || System.currentTimeMillis() - carrierCacheAt > CARRIER_CACHE_MS) {
            cached = List.copyOf(carrierDAO.findActive());
            carrierCache = cached;
            carrierCacheAt = System.currentTimeMillis();
        }
        return cached;
    }

    /** Admin vừa sửa danh sách hãng (F10): bỏ bản lưu tạm để trang thanh toán thấy ngay. */
    public static void clearCarrierCache() {
        carrierCache = null;
    }

    public double calculateFee(String carrierId, String fromCity, String toCity) throws SQLException {
        return requireCarrier(carrierId).calculateFee(fromCity, toCity);
    }

    /**
     * Order.chooseCarrier(carrierId): Shipment — tạo vận đơn và ghi phí ship vào đơn (cùng một transaction).
     * fromCity/toCity để trống thì lấy theo địa chỉ mặc định người bán và địa chỉ giao của đơn.
     * @param codAmount số tiền shipper phải thu; 0 nếu đơn trả trước online
     */
    public Shipment createShipment(String orderId, String carrierId, String fromCity, String toCity,
                                   double codAmount) throws SQLException {
        ShippingCarrier carrier = requireCarrier(carrierId);
        return inTransaction(c -> {
            OrderRef order = requireOrder(c, orderId);
            if (shipmentDAO.findByOrderId(c, orderId) != null) {
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
            shipmentDAO.insert(c, s);
            orderDAO.updateShippingFee(c, orderId, s.getShippingFee());
            return s;
        });
    }

    /**
     * Kiểm tra userId được xem / thao tác với đơn: người mua hoặc người bán của đơn (admin luôn được).
     * sellerOnly = true thì chỉ người bán (dùng cho tạo vận đơn, "Đã gửi hàng", các nút giả lập).
     * @return đơn hàng nếu được phép
     */
    public OrderRef requireAccess(String orderId, String userId, boolean admin, boolean sellerOnly)
            throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            OrderRef order = requireOrder(c, orderId);
            boolean seller = order.sellerId().equals(userId);
            boolean buyer = order.buyerId().equals(userId);
            if (!admin && !seller && (sellerOnly || !buyer)) {
                throw new SecurityException("Bạn không có quyền với đơn hàng này");
            }
            return order;
        }
    }

    public Shipment findByOrder(String orderId) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            return shipmentDAO.findByOrderId(c, orderId);
        }
    }

    /** Người bán bấm "Đã gửi hàng" và nhập mã vận đơn. */
    public void markShipped(String orderId, String trackingCode) throws SQLException {
        changeShipment(orderId, s -> {
            s.setTrackingCode(trackingCode);
            s.updateStatus(ShipmentStatus.PICKED_UP);
        }, true);
    }

    /** Nút giả lập cập nhật trạng thái (không nối API GHN/GHTK thật). */
    public void updateStatus(String orderId, ShipmentStatus newStatus) throws SQLException {
        changeShipment(orderId, s -> s.updateStatus(newStatus), true);
    }

    public void confirmCodCollected(String orderId) throws SQLException {
        changeShipment(orderId, Shipment::confirmCodCollected, false);
    }

    public void confirmCodRemitted(String orderId) throws SQLException {
        changeShipment(orderId, Shipment::confirmCodRemitted, false);
    }

    /**
     * Đọc vận đơn, đổi theo luật trong Shipment, lưu lại; syncOrder = true thì cập nhật luôn trạng thái Order
     * và báo người mua. Tất cả trên 1 kết nối, thông báo gửi sau khi commit.
     */
    private void changeShipment(String orderId, Consumer<Shipment> change, boolean syncOrder) throws SQLException {
        StatusChange done = inTransaction(c -> {
            Shipment s = shipmentDAO.findByOrderId(c, orderId);
            if (s == null) {
                throw new IllegalArgumentException("Đơn " + orderId + " chưa có vận đơn");
            }
            change.accept(s);
            shipmentDAO.update(c, s);
            if (!syncOrder) {
                return null;
            }
            OrderRef order = syncOrder(c, s);
            return order == null ? null : new StatusChange(order, s.getStatus());
        });
        if (done != null) {
            notificationService.send(done.order().buyerId(), NotificationType.SHIPMENT,
                    "Đơn " + done.order().code() + ": " + done.status().label(),
                    "/shipping/track?orderId=" + done.order().id());
        }
    }

    /** Đơn vừa đổi trạng thái theo vận đơn, để báo người mua sau khi commit. */
    private record StatusChange(OrderRef order, ShipmentStatus status) {
    }

    /**
     * Cập nhật trạng thái Order theo vận đơn:
     * picked_up / in_transit → shipping (Order.confirmShipped), delivered → delivered.
     * returned chưa đổi đơn: TV4 xử lý hoàn tiền / khiếu nại.
     * TODO: khi TV3 có OrderService thì gọi confirmShipped()/... của TV3 thay cho OrderRefDAO.
     * @return đơn hàng (để báo người mua), null nếu không tìm thấy
     */
    private OrderRef syncOrder(Connection c, Shipment s) throws SQLException {
        OrderRef order = orderDAO.find(c, s.getOrderId());
        if (order == null) {
            return null;
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
        return order;
    }

    private ShippingCarrier requireCarrier(String carrierId) throws SQLException {
        for (ShippingCarrier carrier : listCarriers()) {
            if (carrier.getId().equals(carrierId)) {
                return carrier;
            }
        }
        throw new IllegalArgumentException("Đơn vị vận chuyển không tồn tại hoặc đã ngừng: " + carrierId);
    }

    private OrderRef requireOrder(Connection c, String orderId) throws SQLException {
        OrderRef order = orderId == null ? null : orderDAO.find(c, orderId);
        if (order == null) {
            throw new IllegalArgumentException("Không tìm thấy đơn hàng " + orderId);
        }
        return order;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @FunctionalInterface
    private interface TxWork<T> {
        T run(Connection c) throws SQLException;
    }

    /** Chạy trong một transaction: lỗi thì rollback toàn bộ. */
    private <T> T inTransaction(TxWork<T> work) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            c.setAutoCommit(false);
            try {
                T result = work.run(c);
                c.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);   // trả kết nối về pool ở trạng thái mặc định cho người mượn sau
            }
        }
    }
}
