# Cunguoimoita.com

Chợ đồ cũ: người dùng đăng bài bán, chat và trả giá, đặt đơn, thanh toán (VNPay hoặc COD), theo dõi vận chuyển, đánh giá, khiếu nại và hoàn tiền. Có trang quản trị cho admin.

## Công nghệ

- Java 17, Maven (đóng gói `war`)
- Jakarta Servlet 6.1, JSP + JSTL 3, chạy trên **Tomcat 10.1 trở lên** (nhóm dùng Tomcat 11). Không dùng `javax.servlet`.
- PostgreSQL. Cả nhóm dùng chung một database trên Neon.
- Truy cập dữ liệu bằng **Connection Pool** (JNDI `jdbc/cunguoimoita`, lớp `common/db/ConnectionPool`) và **PreparedStatement** cho mọi câu SQL.
- Gửi mail bằng Gmail SMTP (angus-mail), thanh toán thử bằng VNPay sandbox.
- JUnit 5 cho test.

## Chạy project

1. Tạo database: chạy `src/main/resources/sql/00_database_chung.sql` trên PostgreSQL (database chung trên Neon đã có sẵn, không cần chạy lại).
2. Copy `src/main/webapp/META-INF/context.xml.example` thành `context.xml` trong cùng thư mục, rồi sửa `url`, `username`, `password`. File này nằm trong `.gitignore`, **không commit**.
   - Các dòng `<Parameter>` trong file là tuỳ chọn. Để nguyên giá trị `ĐỔI_...` thì web vẫn chạy:
     - Email: mã xác nhận được in ra log Tomcat thay vì gửi mail.
     - Google/Facebook: nút đăng nhập báo "chưa được cấu hình".
   - VNPay đọc `VNP_TMN_CODE`/`VNP_HASH_SECRET` (biến môi trường), hoặc `vnpay.tmnCode`/`vnpay.hashSecret` (system property, `vnpay.properties` hay `<Parameter>`).
3. Build bằng `mvn package` (hoặc `mvnw package`), rồi deploy `target/cunguoimoita-1.0-SNAPSHOT.war` lên Tomcat. Trong IntelliJ thì chạy cấu hình Tomcat như bình thường.
4. Mở `/home`. Khách được xem bảng tin, tìm kiếm và chi tiết bài; lưu bài, chat và giỏ hàng thì phải đăng nhập.

Hướng dẫn test từng chức năng có trong `TESTCASE-README.md`.

## Cấu trúc thư mục

```
cunguoimoita.com/
├── pom.xml
├── README.md, TESTCASE-README.md
├── docs/
│   └── Cunguoimoita-Final-Ver-fixed.drawio(.png)   class diagram bản chuẩn của nhóm
└── src/
    ├── main/
    │   ├── java/org/example/cunguoimoita/
    │   │   ├── common/        dùng chung: ConnectionPool, filter đăng nhập, enum dùng chung,
    │   │   │                  util (CurrentUser, FormUtil, JsonUtil, MediaStore, MailSender, Texts...),
    │   │   │                  HomeServlet, MediaServlet, LegacyImageServlet, ScheduledJobs
    │   │   ├── account/       F1  User, Customer, Address: đăng ký, xác nhận email, đăng nhập, Google/Facebook, hồ sơ
    │   │   ├── product/       F2  Product, Category: đăng bài, sửa, bài của tôi
    │   │   ├── feed/          F3  bảng tin, tìm kiếm/lọc, Bucket List (Wishlist)
    │   │   ├── order/         F4  Cart, Order, OrderItem, Voucher
    │   │   ├── payment/       F5  Payment (VNPay, COD), lịch sử thanh toán
    │   │   ├── shipping/      F6  Shipment, ShippingCarrier
    │   │   ├── chat/          F7  Conversation, Message, PriceOffer
    │   │   ├── notification/  F7  Notification
    │   │   ├── review/        F8  Review, MemberTier
    │   │   ├── dispute/       F9  Dispute, Refund, Payout
    │   │   └── admin/         F10 Admin, Report: duyệt bài, báo cáo, khiếu nại, người dùng, voucher, danh mục...
    │   ├── resources/sql/     00_database_chung.sql (toàn bộ schema) và các file bổ sung theo số thứ tự
    │   └── webapp/
    │       ├── META-INF/context.xml.example
    │       ├── WEB-INF/web.xml, WEB-INF/views/<chức năng>/*.jsp
    │       ├── assets/css, assets/js, assets/images
    │       └── index.jsp, login.jsp, register.jsp, verify-email.jsp
    └── test/java/...          test cho thanh toán, khiếu nại, tính tiền trả người bán
```

Mỗi chức năng chia 4 tầng:

- `controller`: Servlet, chỉ đọc request, gọi service rồi forward hoặc redirect.
- `service`: nghiệp vụ và transaction.
- `dao`: SQL, chỉ dùng PreparedStatement, lấy kết nối từ `ConnectionPool`.
- `model`: lớp theo class diagram và enum trạng thái.

Không viết SQL trong servlet hay JSP.

## File SQL

- `00_database_chung.sql`: tạo toàn bộ database (26 bảng theo class diagram và dữ liệu mẫu). Đã gộp sẵn nội dung các file bên dưới.
- `05` đến `12`: từng phần đã gộp vào `00`. Database chung đã tạo trước đó thì chạy file tương ứng một lần. Có thể chạy lại, không mất dữ liệu.
- `14_xoa_danh_muc_trung.sql`: xoá các danh mục bị thêm trùng ngày 20/10/2026, chạy một lần trên Neon.

## Quy ước

- Ảnh mới lưu trong bảng `media` và được phục vụ ở `/media/<id>`. Các đường dẫn ảnh kiểu cũ trên đĩa (`/uploads`, `/product-image`, `/avatar`) vẫn đọc được qua `LegacyImageServlet`.
- Mọi ảnh giao diện nằm trong `src/main/webapp/assets/images/`. Muốn đổi icon thì ghi đè file cùng tên. Icon danh mục đặt tên `category-<tên-không-dấu>.png`.
- Luật chuyển trạng thái đơn nằm ở `Order.Status.canMoveTo`, luật vận đơn ở `ShipmentStatus.canMoveTo`. Tiền chỉ trả cho người bán khi đơn hoàn tất và không có khiếu nại đang mở.
