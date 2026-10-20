package org.example.cunguoimoita.feed.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.cunguoimoita.admin.model.Category;
import org.example.cunguoimoita.common.util.CurrentUser;
import org.example.cunguoimoita.common.util.FormUtil;
import org.example.cunguoimoita.feed.service.FeedService;
import org.example.cunguoimoita.product.model.ListingType;
import org.example.cunguoimoita.product.model.ProductCondition;
import org.example.cunguoimoita.product.model.ProductFilter;
import org.example.cunguoimoita.product.service.ProductService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/** Tìm kiếm và lọc bài đăng (Guest.searchProduct, viewCategory). */
@WebServlet("/feed/search")
public class SearchServlet extends HttpServlet {

    private final FeedService feedService = new FeedService();
    private final ProductService productService = new ProductService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        ProductFilter filter = new ProductFilter();
        filter.setKeyword(FormUtil.text(req, "q"));
        filter.setCategoryId(FormUtil.text(req, "category"));
        filter.setCondition(ProductCondition.fromCode(req.getParameter("condition")));
        filter.setListingType(ListingType.fromCode(req.getParameter("type")));
        filter.setSort(parseSort(req.getParameter("sort")));
        filter.setPage(FormUtil.positiveInt(req, "page", 1));
        try {
            filter.setMinPrice(FormUtil.money(req, "minPrice", "Giá thấp nhất"));
            filter.setMaxPrice(FormUtil.money(req, "maxPrice", "Giá cao nhất"));
        } catch (IllegalArgumentException e) {
            req.setAttribute("filterError", e.getMessage());   // bỏ lọc giá sai, vẫn hiện kết quả
        }
        if (filter.getMinPrice() != null && filter.getMaxPrice() != null
                && filter.getMinPrice().compareTo(filter.getMaxPrice()) > 0) {
            req.setAttribute("filterError", "Giá thấp nhất đang lớn hơn giá cao nhất, mình đã đổi chỗ hai giá");
            var min = filter.getMinPrice();
            filter.setMinPrice(filter.getMaxPrice());
            filter.setMaxPrice(min);
        }

        try {
            List<Category> categories = productService.categories();
            req.setAttribute("categories", categories);
            req.setAttribute("activeCategory", categories.stream()
                    .filter(cg -> cg.getId().equals(filter.getCategoryId())).findFirst().orElse(null));
            req.setAttribute("result", feedService.search(filter, CurrentUser.id(req)));
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        // Nút Lưu trên mỗi bài quay về đúng trang kết quả này
        String query = req.getQueryString();
        req.setAttribute("backUrl", "/feed/search" + (query == null ? "" : "?" + query));
        req.getRequestDispatcher("/WEB-INF/views/feed/search.jsp").forward(req, resp);
    }

    private ProductFilter.Sort parseSort(String s) {
        if ("price_asc".equals(s)) {
            return ProductFilter.Sort.PRICE_ASC;
        }
        if ("price_desc".equals(s)) {
            return ProductFilter.Sort.PRICE_DESC;
        }
        return ProductFilter.Sort.NEWEST;
    }
}
