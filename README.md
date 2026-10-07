# Cunguoimoita.com ReadBook

1. **Công nghệ sử dụng**

- Front-End Development:
  - HTML
  - CSS
  - JavaScript
- Back-end Dev:
  - Java [JDK 8+]
  - Servlet
  - JSP
- Database( Mọn người bàn rồi chọn): ykr PosstgreSQL

2. **Cây thư mục**

**Cấu trúc thư mục:**

cunguoimoita.com/
├── pom.xml
├── .gitignore
├── docs/
│   ├── CAU-TRUC-THU-MUC.md            ← file này
│   ├── diagrams/Cunguoimoita_revised.drawio
│   └── prototype/test.html            ← bản HTML tĩnh cũ (giữ để tham khảo)
└── src/
    ├── main/
    │   ├── java/org/example/cunguoimoita/
    │   │   ├── common/                 ← DÙNG CHUNG (cả nhóm, sửa phải báo)
    │   │   │   ├── config/             (AppContextListener, hằng số)
    │   │   │   ├── db/                 (DBConnection)
    │   │   │   ├── filter/             (EncodingFilter, AuthFilter, AdminFilter)
    │   │   │   ├── model/              (BaseEntity, enum dùng chung)
    │   │   │   ├── util/               (PasswordUtil, DateUtil, UploadUtil, JsonUtil)
    │   │   │   └── HomeServlet.java    (thay HelloServlet cũ)
    │   │   ├── account/      F1  TV1   User, Customer, Guest, Address
    │   │   ├── admin/        F10 TV1   Admin, Report
    │   │   ├── product/      F2  TV2   Product, Category
    │   │   ├── feed/         F3  TV2   Wishlist (Bucket List), tìm kiếm
    │   │   ├── order/        F4  TV3   Cart, CartItem, Order, OrderItem, Voucher
    │   │   ├── review/       F8  TV3   Review, MemberTier
    │   │   ├── payment/      F5  TV4   Payment
    │   │   ├── dispute/      F9  TV4   Dispute, Refund, Payout
    │   │   ├── shipping/     F6  TV5   Shipment, ShippingCarrier
    │   │   ├── chat/         F7  TV5   Conversation, Message, PriceOffer
    │   │   └── notification/ F7  TV5   Notification
    │   ├── resources/
    │   │   ├── db.properties.example   (mỗi người copy thành db.properties, không commit)
    │   │   └── sql/                    (00_common.sql, 01_account.sql … mỗi chức năng 1 file)
    │   └── webapp/
    │       ├── index.jsp               (trang chủ, chuyển sang bảng tin)
    │       ├── assets/
    │       │   ├── css/style.css       (CSS chung)  + css/<chức năng>.css
    │       │   ├── js/                 (common.js + js/<chức năng>.js)
    │       │   └── images/             (logo, icon)
    │       └── WEB-INF/
    │           ├── web.xml
    │           └── views/
    │               ├── common/         (header.jsp, footer.jsp, layout)
    │               ├── account/  admin/  product/  feed/  order/
    │               └── review/  payment/  dispute/  shipping/  chat/
    └── test/java/org/example/cunguoimoita/   (cùng cấu trúc như main)

3. **Phân chia công việc**

| Thành viên | Chức năng A                   | Chức năng B                                   |
| ---------- | ----------------------------- | --------------------------------------------- |
| Tuấn Anh   | F5. Thanh toán                | F9. Khiếu nại, hoàn tiền & trả tiền người bán |
| Phát       | F2. Đăng & quản lý bài đăng   | F3. Bảng tin, tìm kiếm & Bucket List          |
| Thành      | F6. Vận chuyển & theo dõi đơn | F7. Tin nhắn, trả giá & thông báo             |
| Tuyết      | F4. Giỏ hàng & đặt hàng       | F8. Đánh giá & hạng thành viên                |
| Trung      | F1. Tài khoản & hồ sơ         | F10. Trang quản trị (Admin)                   |

**Mô tả công việc:**

- [ ] **F1**: 

---

**Class:** User, Customer, Guest (`register()`), Address

Người dùng tạo tài khoản, đăng nhập và quản lý thông tin của mình.

- **Màn hình:** đăng ký, đăng nhập, đăng xuất; trang hồ sơ cá nhân (xem và sửa tên, SĐT, ảnh đại diện, đổi mật khẩu); sổ địa chỉ (thêm, sửa, chọn địa chỉ mặc định); khai báo tài khoản nhận tiền (`payoutAccountRef`).
- **Phía server:** lưu mật khẩu dạng hash (`passwordHash`, `verifyPassword()`); cấp phiên đăng nhập/token; phân quyền theo `userRole` (customer, admin) để các chức năng khác dùng lại; chặn đăng nhập nếu `status = banned`.
- **Trang hồ sơ công khai** của người bán hiển thị điểm đánh giá, số đơn đã bán, hạng thành viên (số liệu lấy từ F8).
- **Xong khi:** đăng ký → đăng nhập → sửa hồ sơ → thêm 2 địa chỉ và đổi mặc định chạy trơn tru; tài khoản bị khoá không đăng nhập được.

---

- [ ] **F2**:

---

**Class:** Product, Category (chọn danh mục), Customer (`postProduct()`)

Người bán đăng món đồ muốn thanh lý và quản lý các bài của mình.

- **Màn hình:** form đăng bài (nhiều ảnh, tên, mô tả, chọn danh mục cha → con, tình trạng, loại bài bán/trao đổi/cả hai, giá bán; nếu có trao đổi thì nhập giá trị ước tính và món muốn đổi; tick có nhận COD hay không); trang "Bài đăng của tôi" để sửa, ẩn, xoá, xem trạng thái.
- **Phía server:** upload ảnh; lưu bài ở trạng thái chờ admin duyệt (`verifiedStatus = unverified`); quản lý vòng đời `status`: draft → available → reserved (có người đặt) → sold / exchanged, hoặc hidden; hàm `reserve()` và `markSold()` để F4 gọi khi có đơn.
- **Gợi ý trao đổi:** với bài loại swap, `suggestMatches()` trả về các bài swap khác có giá trị ước tính gần bằng.
- **Xong khi:** đăng bài có ảnh, bài hiện "chờ duyệt", admin duyệt xong thì lên bảng tin; sửa/ẩn/xoá được; bài swap có danh sách gợi ý.

---

- [ ] **F3**:

---

**Class:** Guest (`browseProduct()`, `searchProduct()`, `viewCategory()`), Product, Category, Wishlist

Khách và người dùng lướt xem, tìm đồ và lưu lại bài thích.

- **Màn hình:** trang chủ dạng bảng tin (mỗi bài như một post: tên người đăng, ảnh, caption, tag danh mục, tình trạng, trạng thái, giá); thanh tìm kiếm theo từ khoá; lọc theo danh mục, khoảng giá, tình trạng, loại bài; trang chi tiết bài đăng (ảnh lớn, thông tin người bán, các nút: Lưu vào Bucket List, Nhắn tin, Thêm vào giỏ); trang Bucket List.
- **Phía server:** API danh sách có phân trang, chỉ trả bài đã duyệt và đang available; tìm kiếm và lọc; thêm, bỏ bài khỏi Wishlist (`add()`, `remove()`).
- Khách chưa đăng nhập xem được bảng tin, khi bấm lưu, nhắn tin hay mua thì được đưa sang đăng nhập.
- **Xong khi:** khách xem và tìm được bài; người dùng lưu/bỏ lưu được, Bucket List hiển thị đúng.

---

- [ ] **F4**:

---

**Class:** Cart, CartItem, Order, OrderItem, Voucher, Customer (`checkout()`)

Người mua gom món vào giỏ và đặt hàng. Đơn cũng có thể được tạo từ một giá đã chốt trong chat (F7).

- **Màn hình:** giỏ hàng (nhóm theo người bán, xoá món, xoá cả giỏ); trang thanh toán đơn: chọn địa chỉ giao (từ F1), chọn đơn vị vận chuyển và xem phí ship (từ F6), nhập mã voucher, chọn online hay COD, xem tổng tiền; trang "Đơn mua của tôi" và "Đơn bán của tôi" với danh sách và chi tiết đơn.
- **Phía server:**
  - `checkout()` tách giỏ thành **mỗi người bán một Order**, sinh mã đơn, chép giá vào `OrderItem.unitPrice` (giá chốt lúc mua).
  - Kiểm tra voucher (`checkValidity()`: còn hạn, đủ giá trị tối thiểu) rồi `redeem()`.
  - `calculateTotal()` = subTotal + shippingFee − discountAmount.
  - Gọi `Product.reserve()` để món không bị người khác mua trùng; huỷ đơn (`cancel()`) thì trả món về available.
  - Cung cấp hàm tạo đơn từ offer cho F7 gọi: đơn có `offerId` trỏ tới PriceOffer, `unitPrice` bằng giá đã chốt, người mua đi tiếp vào trang thanh toán đơn như bình thường. Đơn mua qua giỏ thì `offerId` để null.
- **Quản lý voucher** (phát voucher cho khách) làm ngay trong chức năng này, giao diện admin đơn giản.
- **Xong khi:** 2 món của 2 người bán trong giỏ → checkout ra 2 đơn đúng tổng tiền, voucher hết hạn bị từ chối, món đã đặt không cho người khác đặt.

---

- [ ] **F5**:

---

**Class:** Payment, Order (`choosePaymentMethod()`), Customer (`canUseCod()`)

Thu tiền người mua cho đơn hàng.

- **Màn hình:** chọn cổng thanh toán (VNPay, MoMo, chuyển khoản); trang chuyển hướng sang cổng; trang kết quả thành công / thất bại; lịch sử thanh toán trong chi tiết đơn.
- **Phía server:** `process()` tạo yêu cầu thanh toán và chuyển hướng sang cổng (dùng **sandbox** của VNPay/MoMo); `handleCallback()` nhận kết quả, gọi `markPaid()` hoặc `markFailed()`; đơn chuyển awaiting_payment → paid. Tiền lúc này được **sàn giữ**, chưa trả cho người bán.
- **COD:** `canUseCod()` chặn COD nếu người mua có `codRefusalCount` cao hoặc bài không nhận COD (`acceptCod`).
- **Xong khi:** thanh toán sandbox thành công đổi đơn sang paid, thất bại thì cho thử lại; người bị chặn COD không thấy lựa chọn COD.

---

- [ ] **F6**:

---

**Class:** Shipment, ShippingCarrier, Order (`chooseCarrier()`, `confirmShipped()`, `confirmReceived()`, `complete()`)

Giao hàng từ người bán tới người mua và cập nhật trạng thái đơn.

- **Màn hình:** danh sách đơn vị vận chuyển kèm phí và số ngày dự kiến (dùng ở trang thanh toán F4); người bán bấm "Đã gửi hàng" và nhập mã vận đơn; trang theo dõi đơn với dòng thời gian trạng thái; người mua bấm "Đã nhận hàng".
- **Phía server:** `calculateFee(fromCity, toCity)`; tạo Shipment khi chọn đơn vị vận chuyển; `updateStatus()` theo chuỗi created → picked_up → in_transit → delivered (hoặc returned); đồng bộ trạng thái Order: paid → shipping → delivered → completed.
- **Tự hoàn tất:** nếu người mua không bấm "Đã nhận" và không khiếu nại trong 7 ngày sau khi giao, đơn tự chuyển completed (để F9 trả tiền người bán).
- **COD:** ghi nhận shipper đã thu tiền (`confirmCodCollected()`) và đã nộp về sàn (`confirmCodRemitted()`).
- Không cần nối API thật của GHN/GHTK; dùng nút giả lập cập nhật trạng thái là đủ cho đồ án.
- **Xong khi:** một đơn đi trọn từ paid đến completed qua giao diện; đơn COD có đủ hai mốc thu và nộp tiền.

---

- [ ] **F7**:

---

**Class:** Conversation, Message, PriceOffer, Notification

Người mua hỏi và trả giá với người bán về món đồ, và hệ thống báo cho người dùng khi có sự kiện.

- **Màn hình:** nút "Nhắn tin" trên bài đăng mở cuộc trò chuyện gắn với món đó; hộp thư (danh sách cuộc trò chuyện, tin mới nhất, số chưa đọc); khung chat gửi chữ và ảnh; biểu tượng chuông và danh sách thông báo.
- **Phía server:** `start()` tạo hoặc mở lại cuộc trò chuyện giữa người hỏi và người bán cho một bài; `sendMessage()`; đánh dấu đã đọc (`markAllRead()`, `markAsRead()`); `close()` khi món đã bán. Nên dùng WebSocket (ví dụ [Socket.IO](http://Socket.IO)) cho tin nhắn tức thời.
- **Trả giá trong chat:**
  - Nút "Trả giá" trong khung chat mở ô nhập số tiền; `makeOffer(amount)` tạo PriceOffer và một Message loại `price_offer` hiển thị thành khung giá có hạn (`expiresAt`).
  - Bên nhận thấy 3 nút: Chấp nhận (`accept()`), Từ chối (`decline()`), Trả giá lại (`counter(amount)`, tạo offer mới và offer cũ chuyển declined).
  - Quy tắc: mỗi cuộc trò chuyện chỉ có 1 offer pending; hết hạn thì chuyển expired. Khi một offer được accept, sản phẩm chuyển reserved (gọi F2) và các offer pending khác của cùng sản phẩm tự động declined.
  - Accept xong gọi hàm tạo đơn từ offer của F4, rồi đưa người mua sang trang thanh toán.
- **Thông báo:** cung cấp một hàm `send(userId, type, content, linkUrl)` cho các chức năng khác gọi: đơn mới, đã thanh toán, đang giao, có đánh giá, bài được duyệt/từ chối, kết quả khiếu nại.
- **Xong khi:** hai tài khoản chat qua lại theo thời gian thực có ảnh; trả giá qua lại 2 lần rồi accept thì ra đúng một đơn với giá đã chốt, offer của người khác trên cùng món bị declined; đặt một đơn thì người bán nhận thông báo và bấm vào mở đúng đơn.

---

- [ ] **F8**:

---

**Class:** Review, MemberTier, Customer (`rateUser()`, `/avgRating`, `/totalSales`), User (`/ecoPoints`)

Hai bên chấm điểm nhau sau giao dịch và người dùng tích điểm lên hạng.

- **Màn hình:** nút "Đánh giá" trên đơn đã hoàn tất (1 đến 5 sao + nhận xét); danh sách đánh giá trên trang hồ sơ người dùng; huy hiệu hạng (Bronze, Silver, Gold, Diamond) và thanh tiến độ điểm.
- **Phía server:**
  - Chỉ cho đánh giá khi đơn completed, **mỗi bên tối đa 1 đánh giá cho 1 đơn** (quan hệ Order → Review 0..2), rating trong khoảng 1–5.
  - Tính `/avgRating`, `/totalSales` từ dữ liệu, không lưu cứng.
  - Cộng ecoPoints khi hoàn tất đơn; xét hạng theo `minEcoPoints`; áp ưu đãi của hạng (`discountRate`, `benefitNote`). Nhóm cần chốt discountRate dùng để giảm gì (giảm giá đơn cho người mua hay giảm phí hoa hồng người bán) rồi báo cho TV3 (F4) và TV4 (F9).
- **Xong khi:** đánh giá xong thì điểm trung bình đổi ngay; đủ điểm thì hạng tự lên.

---

- [ ] **F9**:

---

**Class:** Dispute, Refund, Payout, Admin (`resolveDispute()`)

Xử lý tiền sau khi giao: trả cho người bán nếu ổn, hoàn cho người mua nếu có sự cố.

- **Màn hình:** người mua bấm "Khiếu nại" trên đơn (chọn lý do: không nhận được, sai món, hư hỏng, hàng giả, khác; mô tả; ảnh bằng chứng); người bán xem và gửi bằng chứng phản hồi; admin có trang danh sách khiếu nại để xem hai bên và ra quyết định; người bán có trang "Thu nhập" xem các khoản đã và sẽ nhận.
- **Phía server:**
  - **Payout:** đơn completed thì tạo Payout, `calculateNet()` = grossAmount − commissionFee, `execute()` (giả lập chuyển khoản) vào tài khoản nhận tiền của người bán.
  - **Dispute:** `open()` khi đơn đang giao hoặc đã giao; khi có khiếu nại thì **tạm dừng** tự hoàn tất của F6; trạng thái open → reviewing → resolved_refund / resolved_payout / rejected.
  - **Refund:** admin xử thắng cho người mua thì tạo Refund và gọi `Payment.refund()` (đơn COD không có payment, ghi chú hoàn thủ công); huỷ đơn đã thanh toán cũng hoàn tiền.
- **Xong khi:** đơn bình thường → người bán thấy khoản Payout đúng số tiền đã trừ phí; đơn có khiếu nại → admin chọn hoàn tiền thì đơn chuyển refunded và người mua thấy khoản hoàn.

---

- [ ] **F10**:

---

**Class:** Admin, Report, Category (thêm/sửa), ShippingCarrier (`managePartner()`), MemberTier (`manageMemberTier()`), Product (`approveProduct()`), User (`banUser()`)

Khu vực riêng cho admin vận hành sàn, cùng chức năng báo cáo vi phạm của người dùng.

- **Màn hình phía người dùng:** nút "Báo cáo" trên bài đăng và trên hồ sơ người dùng (chọn lý do: lừa đảo, hàng giả, hàng cấm, spam, khác; mô tả; ảnh).
- **Màn hình phía admin:**
  - Duyệt bài: danh sách bài chờ duyệt, duyệt hoặc từ chối kèm lý do.
  - Báo cáo vi phạm: xem, xử lý (`resolve()`: ẩn bài, khoá người dùng) hoặc bác bỏ.
  - Người dùng: tìm kiếm, khoá/mở khoá tài khoản.
  - Danh mục: thêm, sửa danh mục cha – con (`addCategory()`).
  - Đơn vị vận chuyển: thêm, sửa, bật/tắt, phí cơ bản.
  - Hạng thành viên: sửa ngưỡng điểm và ưu đãi.
  - Trang tổng quan đơn giản: số người dùng, số bài, số đơn, doanh thu phí.
- **Phía server:** mọi API admin kiểm tra quyền `userRole = admin` (dùng phần phân quyền của F1).
- Trang danh sách khiếu nại do TV4 làm trong F9, chỉ gắn vào menu admin của F10.
- **Xong khi:** admin duyệt được bài của F2, khoá được tài khoản (tài khoản đó không đăng nhập được), danh mục và đơn vị vận chuyển mới hiện ngay ở F3 và F6.

---

