package org.example.cunguoimoita.order.service;

import org.example.cunguoimoita.product.dao.ProductDAO.ProductRef;
import org.example.cunguoimoita.common.db.ConnectionPool;
import org.example.cunguoimoita.order.dao.CartDAO;
import org.example.cunguoimoita.order.dao.CartDAO.CartLine;
import org.example.cunguoimoita.order.model.Cart;
import org.example.cunguoimoita.order.model.CartItem;
import org.example.cunguoimoita.product.dao.ProductDAO;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

// Giỏ hàng: Cart kiểm tra luật, CartDAO lưu.
public class CartService {

    private final CartDAO cartDAO = new CartDAO();
    private final ProductDAO productDAO = new ProductDAO();

    // Danh sách món trong giỏ để hiển thị (kèm tên, giá, người bán).
    public List<CartLine> getLines(String userId) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            Cart cart = findOrCreateCart(c, userId);
            return cartDAO.findLines(c, cart.getId());
        }
    }

    public void addToCart(String userId, String productId) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            ProductRef product = productDAO.findRef(c, productId);
            if (product == null) {
                throw new IllegalArgumentException("Sản phẩm không tồn tại");
            }
            if (!product.isAvailable()) {
                throw new IllegalStateException("Sản phẩm đã có người đặt hoặc ngừng bán");
            }
            if (product.sellerId().equals(userId)) {
                throw new IllegalStateException("Bạn không thể mua sản phẩm của chính mình");
            }
            Cart cart = findOrCreateCart(c, userId);
            CartItem item = cart.addItem(productId);
            cartDAO.insertItem(c, item);
        }
    }

    // "Mua ngay": bảo đảm món có trong giỏ (đã có thì thôi) để checkout riêng món đó với giá đang đăng.
    public void ensureInCart(String userId, String productId) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            Cart cart = findOrCreateCart(c, userId);
            if (cart.contains(productId)) {
                return;
            }
        }
        addToCart(userId, productId);
    }

    public void removeFromCart(String userId, List<String> productIds) throws SQLException {
        if (productIds == null || productIds.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn sản phẩm để xoá");
        }
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            Cart cart = findOrCreateCart(c, userId);
            cartDAO.deleteItems(c, cart.getId(), productIds);
        }
    }

    public void removeFromCart(String userId, String productId) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            Cart cart = findOrCreateCart(c, userId);
            cart.removeItem(productId);
            cartDAO.deleteItem(c, cart.getId(), productId);
        }
    }

    public void clearCart(String userId) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            Cart cart = findOrCreateCart(c, userId);
            cart.clearCart();
            cartDAO.clear(c, cart.getId());
        }
    }

    // Mỗi Customer có đúng 1 giỏ: chưa có thì tạo.
    private Cart findOrCreateCart(Connection c, String userId) throws SQLException {
        Cart cart = cartDAO.findByOwner(c, userId);
        return cart != null ? cart : cartDAO.create(c, userId);
    }
}