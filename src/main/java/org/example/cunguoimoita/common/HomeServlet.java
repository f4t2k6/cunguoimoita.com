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

/** Trang chủ: bài đã duyệt, mới nhất trước. */
@WebServlet("/home")
public class HomeServlet extends HttpServlet {

    /** Icon danh mục là assets/images/category-<id>.png hoặc category-<tên không dấu>.png; thêm file là đủ, không cần sửa code. */
    private static final String ICON_DIR = "/assets/images/";
    private static final String DEFAULT_ICON = ICON_DIR + "category-mac-dinh.png";

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
                        "icon", iconOf(cg.getId(), cg.getName())));
            }
        }
        return list;
    }

    private String iconOf(String categoryId, String name) {
        for (String key : new String[] { categoryId, slug(name) }) {
            String path = ICON_DIR + "category-" + key + ".png";
            try {
                if (!key.isEmpty() && getServletContext().getResource(path) != null) return path;
            } catch (java.net.MalformedURLException ignored) {
                // tên lạ không tạo được đường dẫn: thử cách tiếp theo
            }
        }
        return DEFAULT_ICON;
    }

    /** "Cây cảnh" -> "cay-canh": bỏ dấu tiếng Việt, chữ thường, nối bằng dấu gạch. */
    private static String slug(String name) {
        if (name == null) return "";
        String s = java.text.Normalizer.normalize(name.replace('đ', 'd').replace('Đ', 'D'), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase();
        return s.replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }
}
