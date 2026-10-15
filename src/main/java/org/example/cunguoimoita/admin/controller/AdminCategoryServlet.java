package org.example.cunguoimoita.admin.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/**
 * GET  /admin/categories   danh mục cha – con. Attribute gửi sang JSP: categories (List&lt;Category&gt;)
 * POST /admin/categories   field: id (rỗng = thêm mới), name, description, parentId
 */
@WebServlet("/admin/categories")
public class AdminCategoryServlet extends AdminBaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            req.setAttribute("categories", adminService.categories());
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        render(req, resp, "categories.jsp", "categories");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String id = req.getParameter("id");
        handle(req, resp, "/admin/categories", () -> {
            if (id == null || id.isBlank()) {
                adminService.addCategory(req.getParameter("name"), req.getParameter("description"),
                        req.getParameter("parentId"));
                return "Đã thêm danh mục";
            }
            adminService.updateCategory(id, req.getParameter("name"), req.getParameter("description"),
                    req.getParameter("parentId"));
            return "Đã lưu danh mục";
        });
    }
}
