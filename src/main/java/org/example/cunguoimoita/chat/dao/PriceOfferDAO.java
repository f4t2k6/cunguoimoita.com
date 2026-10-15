package org.example.cunguoimoita.chat.dao;

import org.example.cunguoimoita.chat.model.OfferStatus;
import org.example.cunguoimoita.chat.model.PriceOffer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class PriceOfferDAO {

    public void insert(Connection c, PriceOffer o) throws SQLException {
        String sql = "INSERT INTO price_offers (id, conversation_id, proposer_id, amount, status, created_at, expires_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, o.getId());
            ps.setString(2, o.getConversationId());
            ps.setString(3, o.getProposerId());
            ps.setDouble(4, o.getAmount());
            ps.setString(5, o.getStatus().dbValue());
            ps.setTimestamp(6, Timestamp.valueOf(o.getCreatedAt()));
            ps.setTimestamp(7, Timestamp.valueOf(o.getExpiresAt()));
            ps.executeUpdate();
        }
    }

    public void updateStatus(Connection c, PriceOffer o) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE price_offers SET status = ? WHERE id = ?")) {
            ps.setString(1, o.getStatus().dbValue());
            ps.setString(2, o.getId());
            ps.executeUpdate();
        }
    }

    /** Khoá dòng (FOR UPDATE) để hai người không cùng accept/counter một offer. */
    public PriceOffer findByIdForUpdate(Connection c, String id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM price_offers WHERE id = ? FOR UPDATE")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public List<PriceOffer> findByConversation(Connection c, String conversationId) throws SQLException {
        List<PriceOffer> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM price_offers WHERE conversation_id = ?")) {
            ps.setString(1, conversationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    /** Các offer pending của cùng Product, trừ offer đang accept. */
    public List<PriceOffer> findPendingOfSameProduct(Connection c, String productId, String exceptOfferId) throws SQLException {
        String sql = "SELECT po.* FROM price_offers po JOIN conversations cv ON cv.id = po.conversation_id "
                + "WHERE cv.product_id = ? AND po.status = 'pending' AND po.id <> ? FOR UPDATE OF po";
        List<PriceOffer> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, productId);
            ps.setString(2, exceptOfferId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    /**
     * Ghi expired xuống DB cho offer pending đã quá hạn của một cuộc trò chuyện.
     * Chỉ gọi trước khi tạo offer mới (để không vướng uq_one_pending_offer); khi đọc thì map() đã tự coi là expired.
     */
    public int expireDue(Connection c, String conversationId) throws SQLException {
        String sql = "UPDATE price_offers SET status = 'expired'"
                + " WHERE conversation_id = ? AND status = 'pending' AND expires_at < ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, conversationId);
            ps.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            return ps.executeUpdate();
        }
    }

    private PriceOffer map(ResultSet rs) throws SQLException {
        PriceOffer o = new PriceOffer();
        o.setId(rs.getString("id"));
        o.setConversationId(rs.getString("conversation_id"));
        o.setProposerId(rs.getString("proposer_id"));
        o.setAmount(rs.getDouble("amount"));
        o.setStatus(OfferStatus.fromDb(rs.getString("status")));
        o.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        o.setExpiresAt(rs.getTimestamp("expires_at").toLocalDateTime());
        o.expireIfDue();   // quá hạn thì hiện là expired luôn, không cần UPDATE mỗi lần đọc
        return o;
    }
}
