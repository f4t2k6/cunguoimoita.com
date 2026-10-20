package org.example.cunguoimoita.admin.dao;

import org.example.cunguoimoita.admin.model.MemberTier;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Hạng thành viên: admin sửa, ReviewService đọc để xét hạng và ưu đãi. */
public class MemberTierDAO {

    public List<MemberTier> findAll(Connection c) throws SQLException {
        String sql = "SELECT mt.*, (SELECT COUNT(*) FROM customers cu WHERE cu.tier_id = mt.id) AS member_count"
                + " FROM member_tiers mt ORDER BY mt.min_eco_points";
        List<MemberTier> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                MemberTier t = new MemberTier();
                t.setId(rs.getString("id"));
                t.setName(rs.getString("name"));
                t.setMinEcoPoints(rs.getInt("min_eco_points"));
                t.setDiscountRate(rs.getDouble("discount_rate"));
                t.setBenefitNote(rs.getString("benefit_note"));
                t.setMemberCount(rs.getInt("member_count"));
                list.add(t);
            }
        }
        return list;
    }

    /** Admin.manageMemberTier(): sửa ngưỡng điểm, % ưu đãi, ghi chú quyền lợi. @return true nếu có hạng này */
    public boolean update(Connection c, MemberTier t) throws SQLException {
        String sql = "UPDATE member_tiers SET min_eco_points = ?, discount_rate = ?, benefit_note = ? WHERE id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, t.getMinEcoPoints());
            ps.setDouble(2, t.getDiscountRate());
            ps.setString(3, t.getBenefitNote());
            ps.setString(4, t.getId());
            return ps.executeUpdate() > 0;
        }
    }
}
