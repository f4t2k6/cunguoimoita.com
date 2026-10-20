package org.example.cunguoimoita.order.service;

import org.example.cunguoimoita.account.dao.AddressDAO;
import org.example.cunguoimoita.account.model.Address;
import org.example.cunguoimoita.common.db.ConnectionPool;
import org.example.cunguoimoita.dispute.dao.DisputeDAO;
import org.example.cunguoimoita.dispute.model.Payout;
import org.example.cunguoimoita.dispute.model.Refund;
import org.example.cunguoimoita.dispute.model.RefundReason;
import org.example.cunguoimoita.dispute.service.PayoutService;
import org.example.cunguoimoita.dispute.service.RefundService;
import org.example.cunguoimoita.notification.model.NotificationType;
import org.example.cunguoimoita.notification.service.NotificationService;
import org.example.cunguoimoita.order.dao.CartDAO;
import org.example.cunguoimoita.order.dao.CartDAO.CartLine;
import org.example.cunguoimoita.order.dao.OrderDAO;
import org.example.cunguoimoita.order.dao.VoucherDAO;
import org.example.cunguoimoita.order.model.Cart;
import org.example.cunguoimoita.order.model.Order;
import org.example.cunguoimoita.order.model.OrderCard;
import org.example.cunguoimoita.order.model.OrderItem;
import org.example.cunguoimoita.order.model.Voucher;
import org.example.cunguoimoita.payment.service.PaymentService;
import org.example.cunguoimoita.product.dao.ProductDAO;
import org.example.cunguoimoita.product.dao.ProductDAO.ProductRef;
import org.example.cunguoimoita.product.model.ProductStatus;
import org.example.cunguoimoita.review.service.ReviewService;
import org.example.cunguoimoita.shipping.model.CodStatus;
import org.example.cunguoimoita.shipping.model.Shipment;
import org.example.cunguoimoita.shipping.model.ShipmentStatus;
import org.example.cunguoimoita.shipping.service.ShippingService;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Đơn hàng.
// Nghiệp vụ nào ghi nhiều bảng thì chạy trong 1 transaction (inTransaction): lỗi 1 chỗ → huỷ hết.
public class OrderService {

    private final OrderDAO orderDAO = new OrderDAO();
    private final CartDAO cartDAO = new CartDAO();
    private final VoucherDAO voucherDAO = new VoucherDAO();
    private final ProductDAO productDAO = new ProductDAO();
    private final AddressDAO addressDAO = new AddressDAO();
    private final NotificationService notificationService = new NotificationService();
    private final ReviewService reviewService = new ReviewService();
    private final ShippingService shippingService = new ShippingService();
    private final PaymentService paymentService = new PaymentService();
    private final PayoutService payoutService = new PayoutService();
    private final RefundService refundService = new RefundService();
    private final DisputeDAO disputeDAO = new DisputeDAO();

    /** Số ngày sau khi giao mà người mua không bấm "Đã nhận hàng" / không khiếu nại thì đơn tự hoàn tất. */
    public static final int AUTO_COMPLETE_DAYS = 7;

    // Customer.checkout() / Cart.checkout(): tách cả giỏ thành mỗi người bán 1 đơn.
    public List<Order> checkout(String buyerId) throws SQLException {
        return checkout(buyerId, null);
    }

    // Như trên nhưng chỉ đặt các món người mua đã tick chọn (selectedProductIds); món không chọn vẫn ở lại giỏ.
    // selectedProductIds = null → đặt cả giỏ.
    public List<Order> checkout(String buyerId, Collection<String> selectedProductIds) throws SQLException {
        if (selectedProductIds != null && selectedProductIds.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn sản phẩm để mua");
        }
        String addressId = defaultAddressId(buyerId);

        List<Order> orders = inTransaction(c -> {
            Cart cart = cartDAO.findByOwner(c, buyerId);
            if (cart == null || cart.isEmpty()) {
                throw new IllegalStateException("Giỏ hàng đang trống");
            }

            // 1. Gom các món (đã chọn) theo người bán: sellerId → danh sách món của người đó
            Map<String, List<CartLine>> bySeller = new LinkedHashMap<>();
            for (CartLine line : cartDAO.findLines(c, cart.getId())) {
                if (selectedProductIds == null || selectedProductIds.contains(line.productId())) {
                    bySeller.computeIfAbsent(line.sellerId(), k -> new ArrayList<>()).add(line);
                }
            }
            if (bySeller.isEmpty()) {
                throw new IllegalStateException("Các sản phẩm đã chọn không còn trong giỏ hàng");
            }

            // 2. Mỗi người bán → 1 Order
            List<Order> created = new ArrayList<>();
            List<String> orderedProductIds = new ArrayList<>();
            for (Map.Entry<String, List<CartLine>> group : bySeller.entrySet()) {
                Order order = newOrder(buyerId, group.getKey(), addressId, null);
                for (CartLine line : group.getValue()) {
                    reserveProduct(c, line.productId(), line.productName());
                    order.getItems().add(newItem(order.getId(), line.productId(), line.price()));
                    orderedProductIds.add(line.productId());
                }
                order.calculateTotal();
                orderDAO.insert(c, order);
                created.add(order);
            }

            // 3. Bỏ các món đã đặt ra khỏi giỏ
            cartDAO.deleteItems(c, cart.getId(), orderedProductIds);
            return created;
        });

        // Báo người bán sau khi đã lưu thành công
        for (Order order : orders) {
            notify(order.getSellerId(), "Bạn có đơn hàng mới " + order.getCode(), order.getId());
        }
        return orders;
    }

    // PriceOffer.accept(): Order: gọi khi người bán chấp nhận giá trong chat.
    // ChatService đã chuyển sản phẩm sang reserved rồi, nên ở đây chỉ tạo đơn 1 món với giá đã chốt.
    // Nhận Connection để chạy chung transaction với ChatService.
    public Order createFromOffer(Connection c, String offerId, String buyerId, String sellerId,
                                 String productId, double amount) throws SQLException {
        Order order = newOrder(buyerId, sellerId, defaultAddressId(buyerId), offerId);
        order.getItems().add(newItem(order.getId(), productId, amount));
        order.calculateTotal();
        orderDAO.insert(c, order);
        return order;
    }

    // Chi tiết 1 đơn: chỉ người mua hoặc người bán của đơn được xem.
    public Order getOrder(String orderId, String userId) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            Order order = orderDAO.findById(c, orderId);
            requireParticipant(order, userId);
            return order;
        }
    }

    // Tên + ảnh của từng món trong đơn (productId → ProductCard), để trang đơn hiện thay cho mã sản phẩm.
    public Map<String, ProductDAO.ProductCard> getItemCards(Order order) throws SQLException {
        Map<String, ProductDAO.ProductCard> cards = new LinkedHashMap<>();
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            for (OrderItem item : order.getItems()) {
                cards.put(item.getProductId(), productDAO.findCard(c, item.getProductId()));
            }
        }
        return cards;
    }

    // Thẻ đơn (kiểu Shopee) cho trang Đơn mua / Đơn bán.
    public List<OrderCard> getOrderCards(String userId, boolean asSeller) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            return orderDAO.findCards(c, userId, asSeller);
        }
    }

    // Order.chooseAddress(): địa chỉ phải thuộc sổ địa chỉ của chính người mua.
    public void chooseAddress(String orderId, String buyerId, String addressId) throws SQLException {
        boolean owned = addressDAO.findByUserId(buyerId).stream()
                .anyMatch(a -> a.getId().equals(addressId));
        if (!owned) {
            throw new IllegalArgumentException("Địa chỉ không hợp lệ");
        }
        inTransaction(c -> {
            Order order = requireEditableOrder(c, orderId, buyerId);
            order.chooseAddress(addressId);
            orderDAO.update(c, order);
            // Đã chọn hãng vận chuyển → tính lại phí ship theo địa chỉ mới
            Shipment s = shippingService.requote(c, orderId);
            if (s != null) {
                order.chooseCarrier(s.getCarrierId(), s.getShippingFee());
                orderDAO.update(c, order);
            }
            return null;
        });
    }

    // Order.chooseCarrier(carrierId): người mua chọn hãng → ShippingService tạo/đổi vận đơn và tính phí → đơn cộng phí ship vào tổng.
    public void chooseCarrier(String orderId, String buyerId, String carrierId) throws SQLException {
        inTransaction(c -> {
            Order order = requireEditableOrder(c, orderId, buyerId);
            Shipment s = shippingService.chooseCarrier(c, orderId, carrierId);
            order.chooseCarrier(carrierId, s.getShippingFee());
            orderDAO.update(c, order);
            return null;
        });
    }

    // Các hãng kèm phí ship của đúng tuyến (người bán → địa chỉ giao), để trang đơn cho chọn.
    public List<ShippingService.CarrierQuote> getCarrierQuotes(String orderId) throws SQLException {
        return shippingService.quoteForOrder(orderId);
    }

    public Shipment getShipment(String orderId) throws SQLException {
        return shippingService.findByOrder(orderId);
    }

    // Voucher còn dùng được của người mua, để trang đơn gợi ý.
    public List<Voucher> getUsableVouchers(String buyerId) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            return voucherDAO.findUsableByCustomer(c, buyerId);
        }
    }

    // Lý do đơn không được trả COD (null = được), để trang đơn ẩn lựa chọn COD.
    public String getCodProblem(String orderId, String buyerId) throws SQLException {
        return paymentService.codProblem(orderId, buyerId);
    }
    // Sổ địa chỉ của người mua, để trang chi tiết đơn hiện danh sách cho chọn.
    public List<Address> getBuyerAddresses(String buyerId) throws SQLException {
        return addressDAO.findByUserId(buyerId);
    }

    // Order.applyVoucher(code): tìm voucher theo mã → Order kiểm tra luật → lưu cả đơn lẫn voucher.
    public void applyVoucher(String orderId, String buyerId, String code) throws SQLException {
        inTransaction(c -> {
            Order order = requireEditableOrder(c, orderId, buyerId);
            Voucher voucher = voucherDAO.findByCode(c, code == null ? "" : code.trim());
            if (voucher == null) {
                throw new IllegalArgumentException("Mã voucher không tồn tại");
            }
            order.applyVoucher(voucher);
            voucherDAO.updateStatus(c, voucher);
            orderDAO.update(c, order);
            return null;
        });
    }

    // Phải chọn hãng vận chuyển trước để tổng tiền đã gồm phí ship. Online → chờ thanh toán;
    // COD → giữ new, vận đơn ghi số tiền shipper phải thu. Từ đây người mua không sửa đơn được nữa.
    public void choosePaymentMethod(String orderId, String buyerId, Order.PaymentMethod method) throws SQLException {
        Order order = inTransaction(c -> {
            Order o = requireEditableOrder(c, orderId, buyerId);
            if (method == Order.PaymentMethod.COD) {
                String problem = paymentService.codProblem(c, orderId, buyerId);
                if (problem != null) {
                    throw new IllegalStateException(problem);
                }
            }
            o.choosePaymentMethod(method);
            orderDAO.update(c, o);
            shippingService.setCodAmount(c, orderId, method == Order.PaymentMethod.COD ? o.getTotalAmount() : 0);
            if (method == Order.PaymentMethod.COD) {
                shippingService.assignTrackingCode(c, orderId);   // đặt COD xong → tự sinh mã vận đơn
            }
            return o;
        });
        if (method == Order.PaymentMethod.COD) {
            notify(order.getSellerId(), "Đơn " + order.getCode() + " đã xác nhận thanh toán khi nhận hàng (COD), mã vận đơn đã được tạo. Vui lòng xác nhận cách giao hàng cho đơn vị vận chuyển.",
                    order.getId());
        }
    }

    // Order.cancel(): người mua hoặc người bán huỷ → trả sản phẩm về available.
    // Đơn đã thanh toán online → tạo khoản hoàn tiền cho người mua trong cùng transaction.
    public void cancel(String orderId, String userId, String reason) throws SQLException {
        Cancelled result = inTransaction(c -> {
            Order o = orderDAO.findByIdForUpdate(c, orderId);
            requireParticipant(o, userId);
            boolean wasPaid = o.getStatus() == Order.Status.PAID;
            o.cancel(reason);
            orderDAO.update(c, o);
            for (OrderItem item : o.getItems()) {
                productDAO.updateStatus(c, item.getProductId(), ProductStatus.AVAILABLE);
            }
            Refund refund = wasPaid
                    ? refundService.refundOrder(c, o.getId(), o.getBuyerId(), 0, RefundReason.CANCELLED)
                    : null;
            return new Cancelled(o, refund);
        });
        Order order = result.order();
        Refund refund = result.refund();
        String other = userId.equals(order.getBuyerId()) ? order.getSellerId() : order.getBuyerId();
        notify(other, "Đơn " + order.getCode() + " đã bị huỷ: " + reason, order.getId());
        if (refund != null) {
            notify(order.getBuyerId(), "Đã hoàn " + String.format("%,.0f", refund.getAmount()) + " đ cho đơn "
                    + order.getCode() + " vừa huỷ.", order.getId());
        }
    }

    // Customer.confirmReceived() → Order.confirmReceived(): người mua bấm "Đã nhận hàng" → đơn completed.
    // Đơn đang khiếu nại thì không bấm được (chờ admin xử). Đơn COD: bấm nhận hàng nghĩa là đã trả tiền cho shipper.
    public void confirmReceived(String orderId, String buyerId) throws SQLException {
        Completed result = inTransaction(c -> {
            Order o = requireBuyerOrder(c, orderId, buyerId);
            // Chỉ bấm được sau khi vận đơn đã qua các bước: đã nhận vận đơn → đang giao → đã giao
            if (o.getStatus() != Order.Status.DELIVERED) {
                throw new IllegalStateException("Chỉ xác nhận đã nhận hàng được sau khi đơn đã giao");
            }
            if (disputeDAO.hasOpenDispute(c, orderId)) {
                throw new IllegalStateException("Đơn đang có khiếu nại, vui lòng chờ admin xử lý");
            }
            shippingService.collectCodIfPending(c, orderId);
            return new Completed(o, completeOrder(c, o));
        });
        notify(result.order().getSellerId(), "Đơn " + result.order().getCode() + " đã hoàn tất", orderId);
        payoutService.notifyCreated(result.payout());
    }

    /**
     * Order.complete(): chuyển completed, sản phẩm sold, xét hạng 2 bên, tạo khoản trả tiền người bán.
     * Chạy trên Connection của nơi gọi; gọi notifyCreated(payout) sau khi commit.
     */
    public Payout completeOrder(Connection c, Order o) throws SQLException {
        o.confirmReceived();              // shipping → delivered → completed
        orderDAO.update(c, o);
        for (OrderItem item : o.getItems()) {
            productDAO.updateStatus(c, item.getProductId(), ProductStatus.SOLD);
        }
        reviewService.recalculateTier(c, o.getBuyerId());    // cộng điểm và xét hạng cả hai bên
        reviewService.recalculateTier(c, o.getSellerId());
        return payoutService.createForOrder(c, o.getId(), o.getSellerId(), o.getSubTotal());
    }

    /** Đơn giao quá AUTO_COMPLETE_DAYS ngày, chưa bấm nhận và không khiếu nại thì tự hoàn tất; mỗi đơn 1 transaction. */
    public int autoCompleteDelivered() throws SQLException {
        List<String> ids;
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            ids = orderDAO.findDeliveredIdsForAutoComplete(c, AUTO_COMPLETE_DAYS);
        }
        int done = 0;
        for (String id : ids) {
            Completed result = inTransaction(c -> {
                Order o = orderDAO.findByIdForUpdate(c, id);
                if (o == null || o.getStatus() != Order.Status.DELIVERED || disputeDAO.hasOpenDispute(c, id)) {
                    return null;          // trong lúc chờ, người mua đã bấm nhận hoặc vừa mở khiếu nại
                }
                return new Completed(o, completeOrder(c, o));
            });
            if (result != null) {
                Order o = result.order();
                notify(o.getBuyerId(), "Đơn " + o.getCode() + " đã tự hoàn tất sau " + AUTO_COMPLETE_DAYS
                        + " ngày kể từ khi giao. Hãy đánh giá người bán nhé!", o.getId());
                notify(o.getSellerId(), "Đơn " + o.getCode() + " đã tự hoàn tất", o.getId());
                payoutService.notifyCreated(result.payout());
                done++;
            }
        }
        return done;
    }

    // Kết quả trả ra khỏi transaction để gửi thông báo sau khi commit.
    private record Cancelled(Order order, Refund refund) {
    }

    private record Completed(Order order, Payout payout) {
    }

    private Order newOrder(String buyerId, String sellerId, String addressId, String offerId) {
        Order order = new Order();
        order.setId(OrderDAO.newId());
        order.setCode(OrderDAO.codeFor(order.getId()));
        order.setBuyerId(buyerId);
        order.setSellerId(sellerId);
        order.setShippingAddressId(addressId);
        order.setOfferId(offerId);
        order.setPaymentMethod(Order.PaymentMethod.COD);   // mặc định COD, người mua đổi ở trang thanh toán
        order.setOrderDate(LocalDateTime.now());
        return order;
    }

    // Tạo 1 OrderItem với giá chốt = giá hiện tại của sản phẩm.
    private OrderItem newItem(String orderId, String productId, double price) {
        OrderItem item = new OrderItem();
        item.setId(OrderDAO.newId());
        item.setOrderId(orderId);
        item.setProductId(productId);
        item.setQuantity(1);
        item.setUnitPrice(price);
        return item;
    }

    // Khoá dòng sản phẩm, kiểm tra còn bán, rồi chuyển reserved → người khác không đặt trùng.
    private void reserveProduct(Connection c, String productId, String productName) throws SQLException {
        ProductRef product = productDAO.findRefForUpdate(c, productId);
        if (product == null || !product.isAvailable()) {
            throw new IllegalStateException("\"" + productName + "\" đã có người đặt. Hãy xoá món này khỏi giỏ rồi đặt lại.");
        }
        productDAO.updateStatus(c, productId, ProductStatus.RESERVED);
    }

    // Địa chỉ mặc định của người mua (danh sách đã sắp mặc định lên đầu).
    private String defaultAddressId(String buyerId) throws SQLException {
        List<Address> addresses = addressDAO.findByUserId(buyerId);
        if (addresses.isEmpty()) {
            throw new IllegalStateException("Bạn cần thêm địa chỉ giao hàng trước khi đặt hàng");
        }
        return addresses.get(0).getId();
    }

    // Đơn phải tồn tại và người đang đăng nhập là người mua hoặc người bán.
    private void requireParticipant(Order order, String userId) {
        if (order == null) {
            throw new IllegalArgumentException("Không tìm thấy đơn hàng");
        }
        if (!userId.equals(order.getBuyerId()) && !userId.equals(order.getSellerId())) {
            throw new SecurityException("Bạn không có quyền với đơn hàng này");
        }
    }

    // Đọc + khoá đơn, và chỉ người mua của đơn mới được thao tác.
    private Order requireBuyerOrder(Connection c, String orderId, String buyerId) throws SQLException {
        Order order = orderDAO.findByIdForUpdate(c, orderId);
        requireParticipant(order, buyerId);
        if (!buyerId.equals(order.getBuyerId())) {
            throw new SecurityException("Chỉ người mua mới làm được thao tác này");
        }
        return order;
    }

    // Như requireBuyerOrder, thêm: đơn đã xác nhận COD (vận đơn đang chờ thu tiền) thì không sửa được nữa,
    // vì người bán có thể đã gửi hàng theo số tiền đó. Muốn đổi thì huỷ đơn và đặt lại.
    private Order requireEditableOrder(Connection c, String orderId, String buyerId) throws SQLException {
        Order order = requireBuyerOrder(c, orderId, buyerId);
        Shipment s = shippingService.findByOrder(c, orderId);
        if (s != null && (s.getCodStatus() != CodStatus.NONE || s.getStatus() != ShipmentStatus.CREATED)) {
            throw new IllegalStateException("Đơn đã xác nhận COD, không sửa được nữa. Muốn đổi hãy huỷ đơn rồi đặt lại.");
        }
        return order;
    }

    // Gửi thông báo; lỗi thông báo không được làm hỏng nghiệp vụ chính (đơn đã lưu rồi).
    private void notify(String userId, String content, String orderId) {
        try {
            notificationService.send(userId, NotificationType.ORDER, content, "/order/detail?id=" + orderId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FunctionalInterface
    private interface Work<T> {
        T run(Connection c) throws SQLException;
    }

    // Chạy work trong 1 transaction: thành công → commit (lưu hết); lỗi → rollback (huỷ hết).
    private <T> T inTransaction(Work<T> work) throws SQLException {
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
                c.setAutoCommit(true);           // trả kết nối về pool ở trạng thái bình thường
            }
        }
    }
}