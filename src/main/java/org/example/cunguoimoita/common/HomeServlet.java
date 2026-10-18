package org.example.cunguoimoita.common;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.cunguoimoita.admin.model.Category;
import org.example.cunguoimoita.common.util.CurrentUser;
import org.example.cunguoimoita.common.util.FormUtil;
import org.example.cunguoimoita.feed.service.FeedService;
import org.example.cunguoimoita.product.model.ProductFilter;
import org.example.cunguoimoita.product.service.ProductService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * GET /home?page= → trang chủ: các bài đã duyệt, mới nhất trước, mỗi bài hiện như một post.
 *     Attribute gửi sang JSP:
 *       categories: List&lt;Map&gt; {slug, name, icon}   danh mục gốc cho thanh danh mục
 *       posts:      List&lt;Product&gt;                     bài của trang hiện tại
 *       result:     FeedService.Page                  để vẽ nút trang trước / trang sau
 *       backUrl:    trang quay về sau khi bấm Lưu
 */
@WebServlet("/home")
public class HomeServlet extends HttpServlet {

    /** Biểu tượng cho các danh mục có sẵn; danh mục admin mới thêm dùng biểu tượng mặc định. */
    private static final Map<String, String> CATEGORY_ICONS = Map.ofEntries(
            Map.entry("dien-thoai", "📱"), Map.entry("noxh", "🏢"), Map.entry("bat-dong-san", "🏠"),
            Map.entry("xe-co", "🚗"), Map.entry("viec-lam", "👷"), Map.entry("dien-tu", "💻"),
            Map.entry("thu-cung", "🐕"), Map.entry("dien-lanh", "🧊"), Map.entry("noi-that", "🛋️"),
            Map.entry("ban-phim", "⌨️"));
    private static final String DEFAULT_ICON = "🏷️";

    private final FeedService feedService = new FeedService();
    private final ProductService productService = new ProductService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ProductFilter filter = new ProductFilter();
        filter.setPage(FormUtil.positiveInt(req, "page", 1));
        try {
            FeedService.Page result = feedService.search(filter, CurrentUser.id(req));
            req.setAttribute("categories", topCategories(productService.categories()));
            req.setAttribute("posts", result.getItems());
            req.setAttribute("result", result);
            req.setAttribute("backUrl", filter.getPage() > 1 ? "/home?page=" + filter.getPage() : "/home");
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        req.getRequestDispatcher("/WEB-INF/views/feed/home.jsp").forward(req, resp);
    }

    private List<Map<String, String>> topCategories(List<Category> all) {
        List<Map<String, String>> list = new ArrayList<>();
        for (Category cg : all) {
            if (cg.isTopLevel()) {
                list.add(Map.of("slug", cg.getId(), "name", cg.getName(),
                        "icon", CATEGORY_ICONS.getOrDefault(cg.getId(), DEFAULT_ICON)));
            }
        }
        return list;
    }
}
