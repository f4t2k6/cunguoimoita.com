package org.example.cunguoimoita.common;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.cunguoimoita.common.util.CurrentUser;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * GET /home → trang chủ dạng bảng tin.
 *     Attribute gửi sang JSP:
 *       categories: List&lt;Map&gt; {slug, name, icon}
 *       posts:      List&lt;Map&gt; {id, author, sellerId, date, quality (new|used|damaged), qualityLabel, caption, price, photos}
 *       reels:      List&lt;String&gt; tiêu đề reel
 * TODO TV2 (F3): thay dữ liệu mẫu bằng CategoryService và ProductService (bài đã duyệt, mới nhất trước),
 *                giữ nguyên tên attribute để home.jsp không phải sửa.
 */
@WebServlet("/home")
public class HomeServlet extends HttpServlet {

    private static final List<Map<String, String>> CATEGORIES = List.of(
            Map.of("slug", "dien-thoai", "name", "Điện thoại", "icon", "📱"),
            Map.of("slug", "noxh", "name", "NOXH", "icon", "🏢"),
            Map.of("slug", "bat-dong-san", "name", "Bất động sản", "icon", "🏠"),
            Map.of("slug", "xe-co", "name", "Xe cộ", "icon", "🚗"),
            Map.of("slug", "viec-lam", "name", "Việc làm", "icon", "👷"),
            Map.of("slug", "dien-tu", "name", "Điện tử", "icon", "💻"),
            Map.of("slug", "thu-cung", "name", "Thú cưng", "icon", "🐕"),
            Map.of("slug", "dien-lanh", "name", "Điện lạnh", "icon", "🧊"),
            Map.of("slug", "noi-that", "name", "Nội thất, cây cảnh", "icon", "🛋️"));

    private static final List<Map<String, Object>> SAMPLE_POSTS = List.of(
            Map.of("id", "P1", "author", "Mai Thành", "sellerId", "S1", "date", "31 tháng 8",
                    "quality", "new", "qualityLabel", "Mới sử dụng",
                    "caption", "Còn em Atk Ghost Ultimate màu bạc likenew fullbox đẹp 1m7",
                    "price", 1_700_000, "photos", List.of("Ảnh 1", "Ảnh 2")),
            Map.of("id", "P2", "author", "Huy Phát", "sellerId", "S2", "date", "29 tháng 8",
                    "quality", "used", "qualityLabel", "Đã qua sử dụng",
                    "caption", "Pass lại bàn phím cơ 75%, switch đã lube, đổi được tai nghe",
                    "price", 850_000, "photos", List.of("Ảnh 1")));

    private static final List<String> SAMPLE_REELS = List.of("Pizza", "Kẹt xe", "Đập hộp", "Fur", "Góc học tập");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        CurrentUser.id(req); // để ?as=<userId> (đăng nhập tạm) có tác dụng cả ở trang chủ
        req.setAttribute("categories", CATEGORIES);
        req.setAttribute("posts", SAMPLE_POSTS);
        req.setAttribute("reels", SAMPLE_REELS);
        req.getRequestDispatcher("/WEB-INF/views/feed/home.jsp").forward(req, resp);
    }
}
