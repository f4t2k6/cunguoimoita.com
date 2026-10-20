package org.example.cunguoimoita.shipping.service;

import org.example.cunguoimoita.account.dao.AddressDAO;
import org.example.cunguoimoita.account.model.Address;
import org.example.cunguoimoita.common.db.ConnectionPool;
import org.example.cunguoimoita.common.util.Texts;
import org.example.cunguoimoita.notification.model.NotificationType;
import org.example.cunguoimoita.notification.service.NotificationService;
import org.example.cunguoimoita.order.dao.OrderDAO;
import org.example.cunguoimoita.order.dao.OrderDAO.OrderRef;
import org.example.cunguoimoita.order.model.Order;
import org.example.cunguoimoita.shipping.dao.ShipmentDAO;
import org.example.cunguoimoita.shipping.dao.ShippingCarrierDAO;
import org.example.cunguoimoita.shipping.model.CodStatus;
import org.example.cunguoimoita.shipping.model.Shipment;
import org.example.cunguoimoita.shipping.model.ShipmentStatus;
import org.example.cunguoimoita.shipping.model.ShippingCarrier;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;

/** Vận chuyển. Các thao tác chạy trong 1 transaction, thông báo gửi sau khi commit. */
public class ShippingService {

    /** Danh sách hãng vận chuyển rất ít đổi nên giữ trong bộ nhớ 5 phút, khỏi hỏi DB mỗi lần tính phí. */
    private static final long CARRIER_CACHE_MS = 5 * 60 * 1000;
    private static volatile List<ShippingCarrier> carrierCache;
    private static volatile long carrierCacheAt;

    private final ShippingCarrierDAO carrierDAO = new ShippingCarrierDAO();
    private final ShipmentDAO shipmentDAO = new ShipmentDAO();
    private final OrderDAO orderDAO = new OrderDAO();
    private final NotificationService notificationService = new NotificationService();
    private final AddressDAO addressDAO = new AddressDAO();
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Cách người bán bàn giao hàng cho đơn vị vận chuyển. */
    public enum Handover { DROPOFF, PICKUP }

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

    /** Tên hãng để hiển thị (hãng đã ngừng hoạt động vẫn trả về id). */
    public String carrierName(String carrierId) throws SQLException {
        for (ShippingCarrier carrier : listCarriers()) {
            if (carrier.getId().equals(carrierId)) {
                return carrier.getName();
            }
        }
        return carrierId;
    }

    /** Admin vừa sửa danh sách hãng: bỏ bản lưu tạm để trang thanh toán thấy ngay. */
    public static void clearCarrierCache() {
        carrierCache = null;
    }

    /** 1 dòng trong bảng chọn hãng của trang đơn: hãng + phí cho tuyến của đơn. */
    public record CarrierQuote(ShippingCarrier carrier, double fee) {
    }

    /** Các hãng đang hoạt động kèm phí ship tính theo tỉnh người bán → tỉnh giao của đơn. */
    public List<CarrierQuote> quoteForOrder(String orderId) throws SQLException {
        OrderRef order;
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            order = requireOrder(c, orderId);
        }
        List<CarrierQuote> quotes = new ArrayList<>();
        for (ShippingCarrier carrier : listCarriers()) {
            quotes.add(new CarrierQuote(carrier, carrier.calculateFee(order.fromCity(), order.toCity())));
        }
        return quotes;
    }

    /** Chưa có vận đơn thì tạo, chưa gửi hàng thì đổi hãng và tính lại phí; chạy chung transaction với OrderService. */
    public Shipment chooseCarrier(Connection c, String orderId, String carrierId) throws SQLException {
        ShippingCarrier carrier = requireCarrier(carrierId);
        OrderRef order = requireOrder(c, orderId);
        double fee = carrier.calculateFee(order.fromCity(), order.toCity());
        Shipment s = shipmentDAO.findByOrderIdForUpdate(c, orderId);
        if (s == null) {
            s = new Shipment();
            s.setId(UUID.randomUUID().toString());
            s.setOrderId(orderId);
            s.setCarrierId(carrierId);
            s.setShippingFee(fee);
            s.setCreatedDate(LocalDate.now());
            shipmentDAO.insert(c, s);
            return s;
        }
        if (s.getStatus() != ShipmentStatus.CREATED) {
            throw new IllegalStateException("Hàng đã được gửi, không đổi đơn vị vận chuyển được nữa");
        }
        s.setCarrierId(carrierId);
        s.setShippingFee(fee);
        shipmentDAO.update(c, s);
        return s;
    }

    /** Địa chỉ giao của đơn vừa đổi: tính lại phí theo hãng đã chọn. Chưa chọn hãng → null. */
    public Shipment requote(Connection c, String orderId) throws SQLException {
        Shipment s = shipmentDAO.findByOrderId(c, orderId);
        return s == null ? null : chooseCarrier(c, orderId, s.getCarrierId());
    }

    /** COD → số tiền shipper phải thu = tổng đơn; trả online → 0. */
    public void setCodAmount(Connection c, String orderId, double codAmount) throws SQLException {
        Shipment s = shipmentDAO.findByOrderIdForUpdate(c, orderId);
        if (s == null) {
            throw new IllegalStateException("Vui lòng chọn đơn vị vận chuyển trước");
        }
        if (s.getStatus() != ShipmentStatus.CREATED) {
            throw new IllegalStateException("Hàng đã được gửi, không đổi cách thanh toán được nữa");
        }
        s.setCodAmount(codAmount);
        s.setCodStatus(codAmount > 0 ? CodStatus.PENDING : CodStatus.NONE);
        shipmentDAO.update(c, s);
    }

    /** Người mua bấm "Đã nhận hàng" ở đơn COD = đã trả tiền cho shipper: ghi nhận đã thu nếu shipper chưa bấm. */
    public void collectCodIfPending(Connection c, String orderId) throws SQLException {
        Shipment s = shipmentDAO.findByOrderIdForUpdate(c, orderId);
        if (s != null && s.getCodStatus() == CodStatus.PENDING) {
            s.confirmCodCollected();
            shipmentDAO.update(c, s);
        }
    }

    /** Vận đơn của đơn, trên Connection có sẵn (dùng trong transaction của nơi gọi). */
    public Shipment findByOrder(Connection c, String orderId) throws SQLException {
        return shipmentDAO.findByOrderId(c, orderId);
    }

    /** sellerOnly = true thì chỉ người bán của đơn (admin luôn được). @return đơn nếu được phép */
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

    /** Tự sinh mã vận đơn khi người mua đã thanh toán hoặc đặt COD; đã có mã thì giữ nguyên. */
    public String assignTrackingCode(Connection c, String orderId) throws SQLException {
        Shipment s = shipmentDAO.findByOrderIdForUpdate(c, orderId);
        if (s == null) {
            return null;
        }
        if (Texts.isBlank(s.getTrackingCode())) {
            s.setTrackingCode(newTrackingCode(s.getCarrierId()));
            shipmentDAO.update(c, s);
        }
        return s.getTrackingCode();
    }

    /** Mã vận đơn: tiền tố theo hãng + 9 số ngẫu nhiên, VD GHN482913570. */
    private static String newTrackingCode(String carrierId) {
        String prefix = carrierId == null ? "VD" : switch (carrierId) {
            case "ghn" -> "GHN";
            case "ghtk" -> "GHTK";
            case "ahamove" -> "AHA";
            case "viettel" -> "VTP";
            default -> carrierId.replaceAll("[^A-Za-z]", "").toUpperCase(Locale.ROOT);
        };
        return prefix + String.format("%09d", RANDOM.nextInt(1_000_000_000));
    }

    /** Người bán mang hàng ra bưu cục hoặc hẹn lấy tại địa chỉ: vận đơn picked_up, đơn chuyển đang giao. */
    public String confirmHandover(String orderId, String sellerId, Handover method, String pickupAddressId)
            throws SQLException {
        if (method == null) {
            throw new IllegalArgumentException("Vui lòng chọn cách giao hàng cho đơn vị vận chuyển");
        }
        String where;
        if (method == Handover.PICKUP) {
            Address address = addressDAO.findByUserId(sellerId).stream()
                    .filter(a -> a.getId().equals(pickupAddressId)).findFirst().orElse(null);
            if (address == null) {
                throw new IllegalArgumentException("Vui lòng chọn địa chỉ lấy hàng trong sổ địa chỉ của bạn");
            }
            where = "nhân viên vận chuyển sẽ đến lấy hàng tại " + addressText(address);
        } else {
            where = "người bán mang hàng đến bưu cục gần nhất";
        }
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            OrderRef order = requireOrder(c, orderId);
            Shipment s = shipmentDAO.findByOrderId(c, orderId);
            boolean codConfirmed = order.status() == Order.Status.NEW && s != null && s.getCodStatus() == CodStatus.PENDING;
            if (order.status() != Order.Status.PAID && !codConfirmed) {
                throw new IllegalStateException("Người mua chưa thanh toán hoặc chưa xác nhận COD, chưa giao hàng được");
            }
        }
        changeShipment(orderId, s -> {
            if (Texts.isBlank(s.getTrackingCode())) {
                s.setTrackingCode(newTrackingCode(s.getCarrierId()));   // đơn cũ trước khi có tự sinh mã
            }
            s.updateStatus(ShipmentStatus.PICKED_UP);
        }, true);
        return where;
    }

    private static String addressText(Address a) {
        StringBuilder sb = new StringBuilder(a.getAddressLine() == null ? "" : a.getAddressLine());
        for (String part : new String[]{a.getWard(), a.getDistrict(), a.getProvince()}) {
            if (!Texts.isBlank(part)) {
                sb.append(sb.length() > 0 ? ", " : "").append(part);
            }
        }
        return sb.toString();
    }

    /** Chỉ gửi được khi đơn đã trả online (paid) hoặc đã xác nhận COD. */
    public void markShipped(String orderId, String trackingCode) throws SQLException {
        if (Texts.isBlank(trackingCode)) {
            throw new IllegalArgumentException("Vui lòng nhập mã vận đơn");
        }
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            OrderRef order = requireOrder(c, orderId);
            Shipment s = shipmentDAO.findByOrderId(c, orderId);
            boolean codConfirmed = order.status() == Order.Status.NEW && s != null && s.getCodStatus() == CodStatus.PENDING;
            if (order.status() != Order.Status.PAID && !codConfirmed) {
                throw new IllegalStateException("Người mua chưa thanh toán hoặc chưa xác nhận COD, chưa gửi hàng được");
            }
        }
        changeShipment(orderId, s -> {
            s.setTrackingCode(trackingCode.trim());
            s.updateStatus(ShipmentStatus.PICKED_UP);
        }, true);
    }

    /** Nút giả lập cập nhật trạng thái (không nối API GHN/GHTK thật). */
    public void updateStatus(String orderId, ShipmentStatus newStatus) throws SQLException {
        changeShipment(orderId, s -> s.updateStatus(newStatus), true);
    }

    public void confirmCodCollected(String orderId) throws SQLException {
        changeShipment(orderId, s -> {
            // shipper thu tiền lúc đang giao tới người mua (trước khi báo đã giao), không thu khi còn ở kho
            if (s.getStatus() != ShipmentStatus.IN_TRANSIT && s.getStatus() != ShipmentStatus.DELIVERED) {
                throw new IllegalStateException("Chỉ xác nhận thu tiền COD khi đơn đang được giao");
            }
            s.confirmCodCollected();
        }, false);
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
                    "/order/detail?id=" + done.order().id());
        }
    }

    /** Đơn vừa đổi trạng thái theo vận đơn, để báo người mua sau khi commit. */
    private record StatusChange(OrderRef order, ShipmentStatus status) {
    }

    /**
     * picked_up / in_transit → shipping, delivered → delivered; returned chưa đổi đơn (xử lý qua khiếu nại).
     * @return đơn hàng để báo người mua, null nếu không tìm thấy
     */
    private OrderRef syncOrder(Connection c, Shipment s) throws SQLException {
        OrderRef order = orderDAO.findRef(c, s.getOrderId());
        if (order == null) {
            return null;
        }
        switch (s.getStatus()) {
            case PICKED_UP, IN_TRANSIT -> {
                orderDAO.updateStatusIfIn(c, order.id(), Order.Status.SHIPPING,
                        Order.Status.NEW, Order.Status.AWAITING_PAYMENT, Order.Status.PAID);
            }
            case DELIVERED -> {
                orderDAO.updateStatusIfIn(c, order.id(), Order.Status.DELIVERED,
                        Order.Status.NEW, Order.Status.AWAITING_PAYMENT, Order.Status.PAID, Order.Status.SHIPPING);
            }
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
        OrderRef order = orderId == null ? null : orderDAO.findRef(c, orderId);
        if (order == null) {
            throw new IllegalArgumentException("Không tìm thấy đơn hàng " + orderId);
        }
        return order;
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
