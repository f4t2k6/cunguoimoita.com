package org.example.cunguoimoita.account.dao;

import org.example.cunguoimoita.account.model.PublicSellerProfile;
import org.example.cunguoimoita.common.db.ConnectionPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Chỉ trả trường công khai; số liệu F8 được đọc từ view do nhóm F8 cung cấp. */
public class PublicSellerProfileDAO {
    public PublicSellerProfile findByUserId(String userId) throws SQLException {
        String profileSql = "SELECT u.id, u.full_name, u.avatar_path FROM users u "
                + "JOIN customers c ON c.user_id = u.id "
                + "WHERE u.id = ? AND u.user_role = 'customer' AND u.status = 'active'";
        try (Connection c = ConnectionPool.getInstance().getConnection();
             PreparedStatement profilePs = c.prepareStatement(profileSql)) {
            profilePs.setString(1, userId);
            try (ResultSet profile = profilePs.executeQuery()) {
                if (!profile.next()) {
                    return null;
                }
                String fullName = profile.getString("full_name");
                String avatarPath = profile.getString("avatar_path");
                double averageRating = 0;
                long totalSales = 0;
                String memberTier = "Chưa xếp hạng";

                if (hasF8StatsView(c)) {
                    String statsSql = "SELECT avg_rating, total_sales, member_tier "
                            + "FROM seller_public_stats WHERE user_id = ?";
                    try (PreparedStatement statsPs = c.prepareStatement(statsSql)) {
                        statsPs.setString(1, userId);
                        try (ResultSet stats = statsPs.executeQuery()) {
                            if (stats.next()) {
                                averageRating = stats.getDouble("avg_rating");
                                totalSales = stats.getLong("total_sales");
                                String tier = stats.getString("member_tier");
                                if (tier != null && !tier.isBlank()) {
                                    memberTier = tier;
                                }
                            }
                        }
                    }
                }
                return new PublicSellerProfile(userId, fullName, avatarPath,
                        averageRating, totalSales, memberTier);
            }
        }
    }

    private boolean hasF8StatsView(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT to_regclass('public.seller_public_stats') IS NOT NULL");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() && rs.getBoolean(1);
        }
    }
}
