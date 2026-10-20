package org.example.cunguoimoita.admin.dao;

import org.example.cunguoimoita.admin.model.Category;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Danh mục cha – con của bài đăng. */
public class CategoryDAO {

    /** Danh mục cha trước, con ngay dưới cha của nó. */
    public List<Category> findAll(Connection c) throws SQLException {
        String sql = "SELECT cg.*, pr.name AS parent_name,"
                + " (SELECT COUNT(*) FROM products p WHERE p.category_id = cg.id) AS product_count"
                + " FROM categories cg LEFT JOIN categories pr ON pr.id = cg.parent_category_id"
                + " ORDER BY COALESCE(pr.name, cg.name), cg.parent_category_id IS NOT NULL, cg.name";
        List<Category> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Category cg = new Category();
                cg.setId(rs.getString("id"));
                cg.setName(rs.getString("name"));
                cg.setDescription(rs.getString("description"));
                cg.setParentCategoryId(rs.getString("parent_category_id"));
                cg.setParentName(rs.getString("parent_name"));
                cg.setProductCount(rs.getInt("product_count"));
                list.add(cg);
            }
        }
        return list;
    }

    /** true nếu danh mục đang có danh mục con. */
    public boolean hasChildren(Connection c, String id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM categories WHERE parent_category_id = ? LIMIT 1")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** true nếu là danh mục gốc (không có cha), false nếu là danh mục con, null nếu không tồn tại. */
    public Boolean isTopLevel(Connection c, String id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT parent_category_id IS NULL FROM categories WHERE id = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBoolean(1) : null;
            }
        }
    }

    /** Admin.addCategory() */
    public void insert(Connection c, Category cg) throws SQLException {
        String sql = "INSERT INTO categories (id, name, description, parent_category_id) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, cg.getId());
            ps.setString(2, cg.getName());
            ps.setString(3, cg.getDescription());
            ps.setString(4, cg.getParentCategoryId());
            ps.executeUpdate();
        }
    }

    public void update(Connection c, Category cg) throws SQLException {
        String sql = "UPDATE categories SET name = ?, description = ?, parent_category_id = ? WHERE id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, cg.getName());
            ps.setString(2, cg.getDescription());
            ps.setString(3, cg.getParentCategoryId());
            ps.setString(4, cg.getId());
            ps.executeUpdate();
        }
    }
}
