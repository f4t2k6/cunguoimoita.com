package org.example.cunguoimoita.order.service;

import org.example.cunguoimoita.account.dao.AddressDAO;
import org.example.cunguoimoita.account.model.Address;
import org.example.cunguoimoita.common.dao.ProductRefDAO;
import org.example.cunguoimoita.common.dao.ProductRefDAO.ProductRef;
import org.example.cunguoimoita.common.db.ConnectionPool;
import org.example.cunguoimoita.notification.model.NotificationType;
import org.example.cunguoimoita.notification.service.NotificationService;
import org.example.cunguoimoita.order.dao.CartDAO;
import org.example.cunguoimoita.order.dao.CartDAO.CartLine;
import org.example.cunguoimoita.order.dao.OrderDAO;
import org.example.cunguoimoita.order.dao.VoucherDAO;
import org.example.cunguoimoita.order.model.Cart;
import org.example.cunguoimoita.order.model.Order;
import org.example.cunguoimoita.order.model.OrderItem;
import org.example.cunguoimoita.order.model.Voucher;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Service của đơn hàng: nơi ghép Model + DAO thành 1 nghiệp vụ hoàn chỉnh.
// Nghiệp vụ nào ghi nhiều bảng thì chạy trong 1 transaction (inTransaction): lỗi 1 chỗ → huỷ hết.
public class OrderService {

    private final OrderDAO orderDAO = new OrderDAO();
    private final CartDAO cartDAO = new CartDAO();
    private final VoucherDAO voucherDAO = new VoucherDAO();
    private final ProductRefDAO productDAO = new ProductRefDAO();          // bảng products (F2)
    private final AddressDAO addressDAO = new AddressDAO();                // sổ địa chỉ (F1)
    private final NotificationService notificationService = new NotificationService();   // thông báo (F7)

    // ===================== CHECKOUT =====================

    // Customer.checkout() / Cart.checkout(): tách giỏ thành mỗi người bán 1 đơn.
    public List<Order> checkout(String buyerId) throws SQLException {
        String addressId = defaultAddressId(buyerId);

        List<Order> orders = inTransaction(c -> {
            Cart cart = cartDAO.findByOwner(c, buyerId);
            if (cart == null || cart.isEmpty()) {
                throw new IllegalStateException("Giỏ hàng đang trống");
            }

            // 1. Gom các món theo người bán: sellerId → danh sách món của người đó
            Map<String, List<CartLine>> bySeller = new LinkedHashMap<>();
            for (CartLine line : cartDAO.findLines(c, cart.getId())) {
                bySeller.computeIfAbsent(line.sellerId(), k -> new ArrayList<>()).add(line);
            }

            // 2. Mỗi người bán → 1 Order
            List<Order> created = new ArrayList<>();
            List<String> orderedProductIds = new ArrayList<>();
            for (Map.Entry<String, List<CartLine>> group : bySeller.entrySet()) {
                Order order = newOrder(buyerId, group.getKey(), addressId, null);
                for (CartLine line : group.getValue()) {
                    reserveProduct(c, line.productId(), line.productName());   // khoá món, chuyển reserved
                    order.getItems().add(newItem(order.getId(), line.productId(), line.price()));
                    orderedProductIds.add(line.productId());
                }
                order.calculateTotal();          // subTotal + phí ship (0) − giảm giá (0)
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

    // PriceOffer.accept(): Order — F7 gọi hàm này khi người bán chấp nhận giá.
    // F7 đã chuyển sản phẩm sang reserved rồi, nên ở đây chỉ tạo đơn 1 món với giá đã chốt.
    // Nhận Connection để chạy chung transaction với F7.
    public Order createFromOffer(Connection c, String offerId, String buyerId, String sellerId,
                                 String productId, double amount) throws SQLException {
        Order order = newOrder(buyerId, sellerId, defaultAddressId(buyerId), offerId);
        order.getItems().add(newItem(order.getId(), productId, amount));
        order.calculateTotal();
        orderDAO.insert(c, order);
        return order;
    }

    // ===================== XEM ĐƠN =====================

    // Chi tiết 1 đơn: chỉ người mua hoặc người bán của đơn được xem.
    public Order getOrder(String orderId, String userId) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            Order order = orderDAO.findById(c, orderId);
            requireParticipant(order, userId);
            return order;
        }
    }

    public List<Order> getBuyerOrders(String buyerId) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            return orderDAO.findByBuyer(c, buyerId);
        }
    }

    public List<Order> getSellerOrders(String sellerId) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            return orderDAO.findBySeller(c, sellerId);
        }
    }

    // ===================== TRANG THANH TOÁN (người mua) =====================

    // Order.chooseAddress(): địa chỉ phải thuộc sổ địa chỉ của chính người mua.
    public void chooseAddress(String orderId, String buyerId, String addressId) throws SQLException {
        boolean owned = addressDAO.findByUserId(buyerId).stream()
                .anyMatch(a -> a.getId().equals(addressId));
        if (!owned) {
            throw new IllegalArgumentException("Địa chỉ không hợp lệ");
        }
        inTransaction(c -> {
            Order order = requireBuyerOrder(c, orderId, buyerId);
            order.chooseAddress(addressId);
            orderDAO.update(c, order);
            return null;
        });
    }
    // Sổ địa chỉ của người mua, để trang chi tiết đơn hiện danh sách cho chọn.
    public List<Address> getBuyerAddresses(String buyerId) throws SQLException {
        return addressDAO.findByUserId(buyerId);
    }

    // Order.applyVoucher(code): tìm voucher theo mã → Order kiểm tra luật → lưu cả đơn lẫn voucher.
    public void applyVoucher(String orderId, String buyerId, String code) throws SQLException {
        inTransaction(c -> {
            Order order = requireBuyerOrder(c, orderId, buyerId);
            Voucher voucher = voucherDAO.findByCode(c, code == null ? "" : code.trim());
            if (voucher == null) {
                throw new IllegalArgumentException("Mã voucher không tồn tại");
            }
            order.applyVoucher(voucher);         // voucher → used, đơn có discountAmount mới
            voucherDAO.updateStatus(c, voucher);
            orderDAO.update(c, order);
            return null;
        });
    }

    // Order.choosePaymentMethod(): online → chờ thanh toán (F5 xử lý tiếp), COD → giữ new.
    public void choosePaymentMethod(String orderId, String buyerId, Order.PaymentMethod method) throws SQLException {
        inTransaction(c -> {
            Order order = requireBuyerOrder(c, orderId, buyerId);
            order.choosePaymentMethod(method);
            orderDAO.update(c, order);
            return null;
        });
    }

    // ===================== HUỶ / HOÀN TẤT =====================

    // Order.cancel(): người mua hoặc người bán huỷ → trả sản phẩm về available.
    public void cancel(String orderId, String userId, String reason) throws SQLException {
        Order order = inTransaction(c -> {
            Order o = orderDAO.findByIdForUpdate(c, orderId);
            requireParticipant(o, userId);
            o.cancel(reason);
            orderDAO.update(c, o);
            for (OrderItem item : o.getItems()) {
                productDAO.updateStatus(c, item.getProductId(), "available");
            }
            return o;
        });
        String other = userId.equals(order.getBuyerId()) ? order.getSellerId() : order.getBuyerId();
        notify(other, "Đơn " + order.getCode() + " đã bị huỷ: " + reason, order.getId());
    }

    // Customer.confirmReceived() → Order.confirmReceived(): người mua bấm "Đã nhận hàng" → đơn completed.
    public void confirmReceived(String orderId, String buyerId) throws SQLException {
        Order order = inTransaction(c -> {
            Order o = requireBuyerOrder(c, orderId, buyerId);
            o.confirmReceived();
            orderDAO.update(c, o);
            for (OrderItem item : o.getItems()) {
                productDAO.updateStatus(c, item.getProductId(), "sold");
            }
            // F8: cộng ecoPoints + xét hạng cho người mua và người bán sẽ gọi ở đây (làm ở phần review)
            return o;
        });
        notify(order.getSellerId(), "Đơn " + order.getCode() + " đã hoàn tất", order.getId());
    }

    // ===================== HÀM PHỤ =====================

    // Tạo object Order mới (chưa lưu DB).
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
        ProductRef product = productDAO.findForUpdate(c, productId);
        if (product == null || !product.isAvailable()) {
            throw new IllegalStateException("\"" + productName + "\" đã có người đặt. Hãy xoá món này khỏi giỏ rồi đặt lại.");
        }
        productDAO.updateStatus(c, productId, "reserved");
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

    // Gửi thông báo; lỗi thông báo không được làm hỏng nghiệp vụ chính (đơn đã lưu rồi).
    private void notify(String userId, String content, String orderId) {
        try {
            notificationService.send(userId, NotificationType.ORDER, content, "/order/detail?id=" + orderId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ===================== TRANSACTION =====================

    // Một "công việc" chạy trên 1 Connection và trả về kết quả kiểu T.
    @FunctionalInterface
    private interface Work<T> {
        T run(Connection c) throws SQLException;
    }

    // Chạy work trong 1 transaction: thành công → commit (lưu hết); lỗi → rollback (huỷ hết).
    private <T> T inTransaction(Work<T> work) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            c.setAutoCommit(false);              // tắt tự lưu sau mỗi câu SQL
            try {
                T result = work.run(c);
                c.commit();                      // tất cả câu SQL thành công → lưu
                return result;
            } catch (SQLException | RuntimeException e) {
                c.rollback();                    // có lỗi → huỷ mọi thay đổi trong lần này
                throw e;                         // ném tiếp để Servlet hiện thông báo
            } finally {
                c.setAutoCommit(true);           // trả kết nối về pool ở trạng thái bình thường
            }
        }
    }
}