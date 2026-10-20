package org.example.cunguoimoita.product.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import org.example.cunguoimoita.common.util.CurrentUser;
import org.example.cunguoimoita.common.util.FormUtil;
import org.example.cunguoimoita.product.model.ListingType;
import org.example.cunguoimoita.product.model.Product;
import org.example.cunguoimoita.product.model.ProductCondition;
import org.example.cunguoimoita.product.service.ProductService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Form đăng bài và sửa bài (multipart). */
@WebServlet({"/product/new", "/product/edit"})
@MultipartConfig(fileSizeThreshold = 1024 * 1024, maxFileSize = 5 * 1024 * 1024, maxRequestSize = 45 * 1024 * 1024)
public class ProductFormServlet extends HttpServlet {

    private final ProductService productService = new ProductService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!allowed(req, resp)) {
            return;
        }
        try {
            Product product;
            if (isEdit(req)) {
                product = productService.forEdit(CurrentUser.id(req), req.getParameter("id"));
                if (!product.isEditable()) {
                    resp.sendRedirect(req.getContextPath() + "/product/mine?error="
                            + FormUtil.encode("Bài đã có người đặt hoặc đã bị admin ẩn nên không sửa được"));
                    return;
                }
            } else {
                product = new Product();
                product.setCondition(ProductCondition.USED);
                product.setListingType(ListingType.SALE);
            }
            show(req, resp, product, null);
        } catch (IllegalArgumentException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        if (!allowed(req, resp)) {
            return;
        }
        String sellerId = CurrentUser.id(req);
        Product input = new Product();
        try {
            readForm(req, input);
            List<Part> photos = photos(req);
            if (isEdit(req)) {
                String id = req.getParameter("id");
                String[] keep = req.getParameterValues("keepImage");
                productService.update(sellerId, id, input, keep == null ? List.of() : Arrays.asList(keep), photos);
                resp.sendRedirect(req.getContextPath() + "/product/mine?message=" + FormUtil.encode("Đã lưu thay đổi"));
            } else {
                productService.post(sellerId, input, photos);
                resp.sendRedirect(req.getContextPath() + "/product/mine?message="
                        + FormUtil.encode("Đã đăng bài, bài sẽ hiện trên bảng tin sau khi admin duyệt"));
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            // Giữ lại những gì người dùng đã nhập; ảnh cũ (khi sửa) lấy lại từ database
            keepOldImages(req, input);
            show(req, resp, input, e.getMessage());
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    /** Đọc các ô chữ của form vào Product (chưa kiểm tra, ProductService sẽ kiểm tra). */
    private void readForm(HttpServletRequest req, Product p) {
        p.setId(req.getParameter("id"));
        p.setName(FormUtil.text(req, "name"));
        p.setDescription(FormUtil.text(req, "description"));
        // Chọn danh mục con thì bài thuộc danh mục con, không thì thuộc danh mục cha
        String child = FormUtil.text(req, "childCategoryId");
        p.setCategoryId(child != null ? child : FormUtil.text(req, "categoryId"));
        p.setCondition(ProductCondition.fromCode(req.getParameter("condition")));
        p.setListingType(ListingType.fromCode(req.getParameter("listingType")));
        p.setDesiredExchangeNote(FormUtil.text(req, "desiredExchangeNote"));
        p.setAcceptCod(req.getParameter("acceptCod") != null);
        p.setPrice(FormUtil.money(req, "price", "Giá bán"));
        p.setEstimatedValue(FormUtil.money(req, "estimatedValue", "Giá trị ước tính"));
    }

    /** Các file ở ô "photos". Ảnh vượt 5 MB làm Tomcat báo lỗi ngay khi đọc, đổi thành thông báo dễ hiểu. */
    private List<Part> photos(HttpServletRequest req) throws IOException, ServletException {
        List<Part> list = new ArrayList<>();
        try {
            for (Part part : req.getParts()) {
                if ("photos".equals(part.getName()) && part.getSubmittedFileName() != null) {
                    list.add(part);
                }
            }
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException("Mỗi ảnh tối đa 5 MB, tổng dung lượng tối đa 45 MB");
        }
        return list;
    }

    private void keepOldImages(HttpServletRequest req, Product input) throws ServletException {
        if (!isEdit(req)) {
            return;
        }
        try {
            input.setImages(productService.forEdit(CurrentUser.id(req), req.getParameter("id")).getImages());
        } catch (IllegalArgumentException | SQLException e) {
            input.setImages(new ArrayList<>());
        }
    }

    private void show(HttpServletRequest req, HttpServletResponse resp, Product product, String error)
            throws ServletException, IOException {
        try {
            req.setAttribute("categories", productService.categories());
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        req.setAttribute("product", product);
        req.setAttribute("editing", isEdit(req));
        req.setAttribute("error", error);
        req.setAttribute("maxImages", ProductService.MAX_IMAGES);
        req.getRequestDispatcher("/WEB-INF/views/product/form.jsp").forward(req, resp);
    }

    /** Chỉ khách hàng đã đăng nhập mới đăng bài; admin không bán hàng. */
    private boolean allowed(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (CurrentUser.id(req) == null) {
            CurrentUser.redirectToLogin(req, resp);
            return false;
        }
        if (!FormUtil.isCustomer(req)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Tài khoản admin không đăng bài bán");
            return false;
        }
        return true;
    }

    private boolean isEdit(HttpServletRequest req) {
        return req.getServletPath().endsWith("/edit");
    }
}
