package org.example.cunguoimoita.dispute.service;

import org.example.cunguoimoita.common.db.ConnectionPool;
import org.example.cunguoimoita.dispute.dao.DisputeDAO;
import org.example.cunguoimoita.dispute.model.Dispute;
import org.example.cunguoimoita.dispute.model.DisputeReason;
import org.example.cunguoimoita.dispute.model.DisputeStatus;
import org.example.cunguoimoita.dispute.model.Payout;
import org.example.cunguoimoita.dispute.model.Refund;
import org.example.cunguoimoita.dispute.model.RefundReason;
import org.example.cunguoimoita.notification.model.NotificationType;
import org.example.cunguoimoita.notification.service.NotificationService;
import org.example.cunguoimoita.order.dao.OrderDAO;
import org.example.cunguoimoita.order.model.Order;
import org.example.cunguoimoita.order.model.OrderItem;
import org.example.cunguoimoita.order.service.OrderService;
import org.example.cunguoimoita.product.dao.ProductDAO;
import org.example.cunguoimoita.product.model.ProductStatus;
import org.example.cunguoimoita.shipping.model.CodStatus;
import org.example.cunguoimoita.shipping.model.Shipment;
import org.example.cunguoimoita.shipping.service.ShippingService;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Khiếu nại: người mua mở (đơn đã giao) → người bán phản hồi → admin hoàn tiền người mua hoặc trả tiền người bán.
 * Khiếu nại còn mở thì đơn không tự hoàn tất và người mua không bấm "Đã nhận hàng" được.
 */
public class DisputeService {

    private final DisputeDAO disputeDAO = new DisputeDAO();
    private final OrderDAO orderDAO = new OrderDAO();
    private final ProductDAO productDAO = new ProductDAO();
    private final OrderService orderService = new OrderService();
    private final ShippingService shippingService = new ShippingService();
    private final RefundService refundService = new RefundService();
    private final PayoutService payoutService = new PayoutService();
    private final NotificationService notificationService = new NotificationService();

    /** Kết quả phán quyết, để gửi thông báo sau khi commit. */
    private record Resolved(Dispute dispute, Refund refund, Payout payout) {
    }

    @FunctionalInterface
    private interface Work<T> {
        T run(Connection c) throws SQLException;
    }

    /** Chỉ mở được khi đơn đã giao và chưa có khiếu nại nào đang mở. */
    public Dispute openDispute(String orderId, String buyerId, DisputeReason reason,
                               String description, String evidenceImages) throws SQLException {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Vui lòng mô tả vấn đề của đơn hàng");
        }
        Dispute d = inTransaction(c -> {
            Order order = requireOrderForUpdate(c, orderId);
            if (!order.getBuyerId().equals(buyerId)) {
                throw new SecurityException("Chỉ người mua của đơn mới mở khiếu nại được");
            }
            // Trả hàng/hoàn tiền chỉ mở được khi vận đơn đã giao xong (đã nhận vận đơn → đang giao → đã giao)
            if (order.getStatus() != Order.Status.DELIVERED) {
                throw new IllegalStateException("Chỉ yêu cầu trả hàng/hoàn tiền được sau khi đơn đã giao");
            }
            if (disputeDAO.hasOpenDispute(c, orderId)) {
                throw new IllegalStateException("Đơn này đang có khiếu nại chờ xử lý");
            }
            Dispute nd = new Dispute();
            nd.setId(UUID.randomUUID().toString());
            nd.setOrderId(orderId);
            nd.setRaisedById(buyerId);
            nd.setAgainstId(order.getSellerId());
            nd.setReason(reason);
            nd.setDescription(description.trim());
            nd.setEvidenceImages(evidenceImages);
            nd.setStatus(DisputeStatus.OPEN);
            nd.setCreatedDate(LocalDate.now());
            disputeDAO.insert(c, nd);
            return nd;
        });
        notify(d.getAgainstId(), NotificationType.ORDER,
                "Đơn hàng của bạn đang có khiếu nại (" + reason.getDisplayName() + "). Vui lòng gửi phản hồi và ảnh bằng chứng.",
                d.getId());
        return d;
    }

    /** Người bán (bên bị khiếu nại) gửi lời giải thích + ảnh bằng chứng. */
    public void respond(String disputeId, String sellerId, String response, String images) throws SQLException {
        Dispute d = inTransaction(c -> {
            Dispute x = requireDisputeForUpdate(c, disputeId);
            if (!x.getAgainstId().equals(sellerId)) {
                throw new SecurityException("Chỉ người bán của đơn mới gửi phản hồi được");
            }
            x.respond(response, images);
            disputeDAO.update(c, x);
            return x;
        });
        notify(d.getRaisedById(), NotificationType.ORDER, "Người bán đã phản hồi khiếu nại của bạn.", d.getId());
    }

    /** Dispute.submitEvidence(): người mua nộp thêm ảnh bằng chứng khi khiếu nại còn mở. */
    public void submitEvidence(String disputeId, String buyerId, String newImages) throws SQLException {
        if (newImages == null || newImages.isBlank()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất 1 ảnh");
        }
        inTransaction(c -> {
            Dispute d = requireDisputeForUpdate(c, disputeId);
            if (!d.getRaisedById().equals(buyerId)) {
                throw new SecurityException("Chỉ người mở khiếu nại mới nộp thêm ảnh được");
            }
            d.requireOpen();
            d.submitEvidence(newImages);
            disputeDAO.update(c, d);
            return null;
        });
    }

    /** Admin đưa tranh chấp vào trạng thái thẩm định. */
    public void reviewDispute(String disputeId, String adminId) throws SQLException {
        inTransaction(c -> {
            Dispute d = requireDisputeForUpdate(c, disputeId);
            d.review();
            d.setHandledByAdminId(adminId);
            disputeDAO.update(c, d);
            return null;
        });
    }

    /**
     * Người mua thắng: đơn refunded, hoàn tiền người mua, ẩn sản phẩm để người bán tự đăng lại.
     * @return khoản hoàn tiền, null nếu người mua chưa trả đồng nào
     */
    public Refund resolveForBuyer(String disputeId, String adminId, String resolutionNote) throws SQLException {
        Resolved r = inTransaction(c -> {
            Dispute d = requireDisputeForUpdate(c, disputeId);
            d.resolveForBuyer(adminId, resolutionNote);
            disputeDAO.update(c, d);

            Order order = requireOrderForUpdate(c, d.getOrderId());
            order.setStatus(requireRefundable(order));
            orderDAO.update(c, order);
            for (OrderItem item : order.getItems()) {
                productDAO.updateStatus(c, item.getProductId(), ProductStatus.HIDDEN);
            }
            Shipment s = shippingService.findByOrder(c, order.getId());
            boolean codPaid = s != null && (s.getCodStatus() == CodStatus.COLLECTED || s.getCodStatus() == CodStatus.REMITTED);
            Refund refund = refundService.refundOrder(c, order.getId(), d.getRaisedById(),
                    codPaid ? s.getCodAmount() : 0, RefundReason.DISPUTE_WON);
            return new Resolved(d, refund, null);
        });

        Dispute d = r.dispute();
        String money = r.refund() == null ? "" : " Đã hoàn " + String.format("%,.0f", r.refund().getAmount()) + " đ.";
        notify(d.getRaisedById(), NotificationType.PAYMENT,
                "Khiếu nại của bạn được chấp nhận." + money, d.getId());
        notify(d.getAgainstId(), NotificationType.ORDER,
                "Khiếu nại đơn hàng được xử lý: hoàn tiền cho người mua. Bài đăng đã được ẩn, bạn có thể mở lại trong Bài của tôi.",
                d.getId());
        return r.refund();
    }

    /** Người bán thắng: đơn hoàn tất như bình thường và tạo khoản trả tiền người bán. */
    public void resolveForSeller(String disputeId, String adminId, String resolutionNote) throws SQLException {
        Resolved r = inTransaction(c -> {
            Dispute d = requireDisputeForUpdate(c, disputeId);
            d.resolveForSeller(adminId, resolutionNote);
            disputeDAO.update(c, d);
            Order order = requireOrderForUpdate(c, d.getOrderId());
            Payout payout = null;
            if (order.getStatus() == Order.Status.SHIPPING || order.getStatus() == Order.Status.DELIVERED) {
                shippingService.collectCodIfPending(c, order.getId());
                payout = orderService.completeOrder(c, order);
            }
            return new Resolved(d, null, payout);
        });

        Dispute d = r.dispute();
        payoutService.notifyCreated(r.payout());
        notify(d.getAgainstId(), NotificationType.PAYMENT,
                "Khiếu nại được xử lý có lợi cho bạn, đơn hàng đã hoàn tất.", d.getId());
        notify(d.getRaisedById(), NotificationType.ORDER,
                "Khiếu nại đơn hàng của bạn không được chấp nhận, đơn hàng đã hoàn tất.", d.getId());
    }

    /** Từ chối khiếu nại (không đủ căn cứ): đơn tiếp tục như bình thường, người mua có thể bấm nhận hoặc đơn tự hoàn tất. */
    public void rejectDispute(String disputeId, String adminId, String resolutionNote) throws SQLException {
        Dispute d = inTransaction(c -> {
            Dispute x = requireDisputeForUpdate(c, disputeId);
            x.reject(adminId, resolutionNote);
            disputeDAO.update(c, x);
            return x;
        });
        notify(d.getRaisedById(), NotificationType.ORDER,
                "Khiếu nại đơn hàng của bạn bị từ chối: " + (resolutionNote == null ? "" : resolutionNote), d.getId());
    }

    /** Ném SecurityException nếu người xem không phải người mua, người bán của khiếu nại hoặc admin. */
    public Dispute getForUser(String disputeId, String userId, boolean admin) throws SQLException {
        Dispute d = getById(disputeId);
        if (d == null || (!admin && !d.getRaisedById().equals(userId) && !d.getAgainstId().equals(userId))) {
            throw new SecurityException("Không tìm thấy khiếu nại");
        }
        return d;
    }

    /** @return null nếu người mua mở khiếu nại được, ngược lại là lý do */
    public String openProblem(String orderId, String buyerId) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            Order order = orderDAO.findById(c, orderId);
            if (order == null || !order.getBuyerId().equals(buyerId)) {
                return "Chỉ người mua của đơn mới mở khiếu nại được";
            }
            if (order.getStatus() != Order.Status.DELIVERED) {
                return "Chỉ yêu cầu trả hàng/hoàn tiền được sau khi đơn đã giao";
            }
            if (disputeDAO.hasOpenDispute(c, orderId)) {
                return "Đơn này đang có khiếu nại chờ xử lý";
            }
            return null;
        }
    }

    public Dispute getById(String id) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            return disputeDAO.findById(c, id);
        }
    }

    public Dispute getByOrderId(String orderId) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            return disputeDAO.findByOrderId(c, orderId);
        }
    }

    public List<Dispute> getByUser(String userId) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            return disputeDAO.findByUser(c, userId);
        }
    }

    public List<Dispute> listPendingForAdmin() throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            return disputeDAO.listPendingForAdmin(c);
        }
    }

    public List<Dispute> listAll(int limit) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            return disputeDAO.listAll(c, limit);
        }
    }

    private Dispute requireDisputeForUpdate(Connection c, String disputeId) throws SQLException {
        Dispute d = disputeId == null ? null : disputeDAO.findByIdForUpdate(c, disputeId);
        if (d == null) {
            throw new IllegalArgumentException("Không tìm thấy khiếu nại");
        }
        return d;
    }

    private Order requireOrderForUpdate(Connection c, String orderId) throws SQLException {
        Order order = orderId == null ? null : orderDAO.findByIdForUpdate(c, orderId);
        if (order == null) {
            throw new IllegalArgumentException("Không tìm thấy đơn hàng");
        }
        return order;
    }

    /** Đơn phải đang giao / đã giao / đã thanh toán mới chuyển refunded được (luật canMoveTo của Order). */
    private Order.Status requireRefundable(Order order) {
        if (!order.getStatus().canMoveTo(Order.Status.REFUNDED)) {
            throw new IllegalStateException("Đơn " + order.getCode() + " không thể hoàn tiền ở trạng thái " + order.getStatus().label());
        }
        return Order.Status.REFUNDED;
    }

    // Thông báo dẫn tới trang khiếu nại; lỗi thông báo không làm hỏng nghiệp vụ đã lưu.
    private void notify(String userId, NotificationType type, String content, String disputeId) {
        try {
            notificationService.send(userId, type, content, "/dispute?id=" + disputeId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** Chạy trong một transaction: lỗi thì rollback toàn bộ. */
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
                c.setAutoCommit(true);
            }
        }
    }
}
