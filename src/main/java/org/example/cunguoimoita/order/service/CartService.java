package org.example.cunguoimoita.order.service;

import org.example.cunguoimoita.common.dao.ProductRefDAO;
import org.example.cunguoimoita.common.dao.ProductRefDAO.ProductRef;
import org.example.cunguoimoita.common.db.ConnectionPool;
import org.example.cunguoimoita.order.dao.CartDAO;
import org.example.cunguoimoita.order.dao.CartDAO.CartLine;
import org.example.cunguoimoita.order.model.Cart;
import org.example.cunguoimoita.order.model.CartItem;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

// Service của giỏ hàng: Servlet gọi vào đây, đây gọi Model (Cart) để kiểm tra luật rồi gọi DAO để lưu.
// Mỗi hàm tự mượn 1 Connection từ pool của nhóm và trả lại khi xong (try-with-resources).
public class CartService {

    private final CartDAO cartDAO = new CartDAO();
    private final ProductRefDAO productDAO = new ProductRefDAO();   // DAO tạm của nhóm cho bảng products (F2)

    // Danh sách món trong giỏ để hiển thị (kèm tên, giá, người bán).
    public List<CartLine> getLines(String userId) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            Cart cart = findOrCreateCart(c, userId);
            return cartDAO.findLines(c, cart.getId());
        }
    }

    // Thêm sản phẩm vào giỏ.
    public void addToCart(String userId, String productId) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            ProductRef product = productDAO.find(c, productId);
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
            CartItem item = cart.addItem(productId);      // Model kiểm tra trùng món
            cartDAO.insertItem(c, item);                  // DAO lưu xuống DB
        }
    }

    // Bỏ 1 sản phẩm khỏi giỏ.
    public void removeFromCart(String userId, String productId) throws SQLException {
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            Cart cart = findOrCreateCart(c, userId);
            cart.removeItem(productId);                   // Model báo lỗi nếu món không có trong giỏ
            cartDAO.deleteItem(c, cart.getId(), productId);
        }
    }

    // Xoá cả giỏ.
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