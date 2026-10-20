package org.example.cunguoimoita.shipping.service;

import org.example.cunguoimoita.account.dao.AddressDAO;
import org.example.cunguoimoita.account.model.Address;
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
import java.util.ArrayList;
import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Nghiệp vụ F6. Các chức năng khác gọi vào đây, không gọi thẳng DAO:
 *  - F4 trang chi tiết đơn: quoteForOrder(orderId) để hiện các hãng kèm phí ship của đúng tuyến
 *  - F4 Order.chooseCarrier(carrierId): chooseCarrier(c, orderId, carrierId) tạo/đổi vận đơn trong transaction của F4
 *  - F4 xác nhận COD / thanh toán online: setCodAmount(c, orderId, amount)
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

    /** Admin vừa sửa danh sách hãng (F10): bỏ bản lưu tạm để trang thanh toán thấy ngay. */
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

    /**
     * Người mua chọn (hoặc đổi) hãng vận chuyển ở trang đơn, trước khi thanh toán.
     * Chưa có vận đơn → tạo mới; đã có nhưng chưa gửi hàng → đổi hãng và tính lại phí.
     * Chạy trên Connection của OrderService để chung transaction với việc ghi phí vào đơn.
     * @return vận đơn sau khi chọn (phí ship nằm trong getShippingFee())
     */
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

    /**
     * Ghi số tiền shipper phải thu: COD → tổng đơn (chờ thu), trả online → 0.
     * Gọi khi người mua xác nhận COD hoặc chuyển sang thanh toán online.
     */
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

    /** Vận đơn của đơn, trên Connection có sẵn (F4/F9 dùng trong transaction của mình). */
    public Shipment findByOrder(Connection c, String orderId) throws SQLException {
        return shipmentDAO.findByOrderId(c, orderId);
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

    /**
     * Người mua thanh toán xong (online thành công hoặc đặt COD) → tự sinh mã vận đơn theo hãng đã chọn,
     * vận đơn vẫn ở "Chờ người bán xác nhận" (created). Chạy trong transaction của nơi gọi (F4, F5).
     * Đã có mã thì giữ nguyên. @return mã vận đơn (null nếu đơn chưa có vận đơn)
     */
    public String assignTrackingCode(Connection c, String orderId) throws SQLException {
        Shipment s = shipmentDAO.findByOrderIdForUpdate(c, orderId);
        if (s == null) {
            return null;
        }
        if (isBlank(s.getTrackingCode())) {
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

    /**
     * Người bán xác nhận bàn giao hàng (mã vận đơn đã tự sinh khi người mua thanh toán):
     *  - DROPOFF: tự mang đến bưu cục gần nhất của hãng
     *  - PICKUP:  nhân viên hãng đến lấy tại 1 địa chỉ trong sổ địa chỉ của người bán (pickupAddressId)
     * Vận đơn chuyển sang "Chuẩn bị vận chuyển" (picked_up), đơn chuyển "Đang giao", người mua nhận thông báo.
     * @return dòng mô tả cách bàn giao để hiện cho người bán
     */
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
            boolean codConfirmed = "new".equals(order.status()) && s != null && s.getCodStatus() == CodStatus.PENDING;
            if (!"paid".equals(order.status()) && !codConfirmed) {
                throw new IllegalStateException("Người mua chưa thanh toán hoặc chưa xác nhận COD, chưa giao hàng được");
            }
        }
        changeShipment(orderId, s -> {
            if (isBlank(s.getTrackingCode())) {
                s.setTrackingCode(newTrackingCode(s.getCarrierId()));   // đơn cũ trước khi có tự sinh mã
            }
            s.updateStatus(ShipmentStatus.PICKED_UP);
        }, true);
        return where;
    }

    private static String addressText(Address a) {
        StringBuilder sb = new StringBuilder(a.getAddressLine() == null ? "" : a.getAddressLine());
        for (String part : new String[]{a.getWard(), a.getDistrict(), a.getProvince()}) {
            if (!isBlank(part)) {
                sb.append(sb.length() > 0 ? ", " : "").append(part);
            }
        }
        return sb.toString();
    }

    /**
     * Người bán bấm "Đã gửi hàng" và nhập mã vận đơn.
     * Chỉ gửi được khi người mua đã trả tiền online (đơn paid) hoặc đã xác nhận COD (đơn new, COD chờ thu).
     */
    public void markShipped(String orderId, String trackingCode) throws SQLException {
        if (isBlank(trackingCode)) {
            throw new IllegalArgumentException("Vui lòng nhập mã vận đơn");
        }
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            OrderRef order = requireOrder(c, orderId);
            Shipment s = shipmentDAO.findByOrderId(c, orderId);
            boolean codConfirmed = "new".equals(order.status()) && s != null && s.getCodStatus() == CodStatus.PENDING;
            if (!"paid".equals(order.status()) && !codConfirmed) {
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
                    "/order/detail?id=" + done.order().id());      // người mua xem ở trang chi tiết đơn
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
