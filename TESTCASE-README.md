# Testcase kiểm thử thủ công – Cunguoimoita

Tài liệu liệt kê các testcase để kiểm tra bằng tay mọi chức năng (F1–F10) và các thay đổi mới
(chế độ khách, giỏ hàng tick chọn, Mua ngay, nhận hàng/trả hàng sau khi đã giao, chỉ người mua đánh giá,
hạng thành viên trong hồ sơ, khung trang chung, mã vận đơn tự sinh + người bán xác nhận giao hàng,
lịch sử thanh toán riêng, menu trái có Bài đăng / Đơn mua / Đơn bán).

Cách dùng: làm theo từng bảng, ghi **Đạt / Lỗi** (kèm ghi chú) vào cột *Kết quả*.
Các dòng thông báo trong cột *Kết quả mong đợi* lấy đúng chữ trong code, nên giao diện phải hiện y như vậy.

---

## 0. Chuẩn bị

### 0.1 Tài khoản mẫu (mật khẩu đều là `123456`)

| Mã | Email | Vai trò | Ghi chú |
|---|---|---|---|
| A1 | admin@cunguoimoita.com | admin | trang quản trị |
| S1 | s1@cunguoimoita.com | customer | người bán chính (địa chỉ TP.HCM) |
| S2 | s2@cunguoimoita.com | customer | người bán thứ 2 (Hà Nội) |
| B1 | b1@cunguoimoita.com | customer | người mua chính, có voucher `GIAM50K` (giảm 50.000đ, đơn từ 500.000đ) và `HETHAN20K` (đã hết hạn) |
| B2 | b2@cunguoimoita.com | customer | người mua thứ 2 (dùng để thử quyền / tranh mua) |

### 0.2 Cơ sở dữ liệu
- Lần đầu: chạy lần lượt các file trong `src/main/resources/sql/` theo số thứ tự (`00_` → `12_`) trên Neon/PostgreSQL.
- Các thay đổi đợt này **không cần chạy thêm SQL**.
- `META-INF/context.xml` phải trỏ đúng DB đang dùng.

### 0.3 Tạo bài đang bán để test (hiện Neon chưa có bài nào đang hiện)
Cách chuẩn (cũng là testcase F2):
1. Đăng nhập **S1** → *Đăng bài* → tạo ít nhất 3 bài **Bán** (có 1 bài *không nhận COD*), 1 bài **Trao đổi**, 1 bài **Bán hoặc đổi**, mỗi bài ≥ 1 ảnh.
2. Đăng nhập **S2** → tạo 2 bài **Bán**.
3. Đăng nhập **A1** → *Quản trị → Bài đăng* → **Duyệt** tất cả.

Cách nhanh (chỉ khi cần gấp, chạy trên DB): cho bài mẫu P1 hiện lại
```sql
UPDATE products SET approved_by_admin_id = 'A1', verified_status = 'unverified', status = 'available'
WHERE id IN ('P1', 'P2');
```

### 0.4 Quy ước
- *Khách* = trình duyệt chưa đăng nhập (nên dùng cửa sổ ẩn danh).
- `{ctx}` = đường dẫn gốc của web, ví dụ `http://localhost:8080/cunguoimoita`.
- Sau khi sửa CSS/JS mà giao diện chưa đổi: bấm **Ctrl+F5**.

---

## 1. Chế độ khách & chặn đăng nhập (LoginRequiredFilter)

| Mã TC | Tiền điều kiện | Các bước | Dữ liệu | Kết quả mong đợi | Kết quả |
|---|---|---|---|---|---|
| G-01 | Khách | Mở `{ctx}/` | – | Vào thẳng **Trang chủ** (không bị đẩy sang /login). Góc phải có nút **Đăng nhập** và **Đăng ký**, không có nút Thông báo | |
| G-02 | Khách | Lướt bảng tin, bấm danh mục trên thanh danh mục | – | Xem được danh sách bài, ảnh bài hiện đủ | |
| G-03 | Khách | Gõ từ khoá vào ô tìm kiếm, Enter | "bàn phím" | Trang kết quả tìm kiếm hiện bình thường | |
| G-04 | Khách | Bấm vào 1 bài | Bài đang bán | Mở trang chi tiết; có dòng "Bạn đang xem với tư cách khách…" | |
| G-05 | Khách, đang ở trang chi tiết bài | Bấm **Thêm vào giỏ** | – | Chuyển tới /login; đăng nhập B1 xong **quay lại đúng trang chi tiết bài đó** | |
| G-06 | Khách | Ở bảng tin bấm **Lưu** trên 1 bài | – | Chuyển tới /login; đăng nhập xong quay về trang chủ | |
| G-07 | Khách | Bấm **Nhắn tin** trên 1 bài | – | Chuyển tới /login | |
| G-08 | Khách | Bấm các mục menu trái: Giỏ hàng / Đơn mua / Đơn bán / Tin nhắn / Đã lưu / Đăng bài | – | Mỗi mục đều chuyển tới /login; đăng nhập xong vào đúng trang vừa bấm | |
| G-20 | Khách hoặc đã đăng nhập | Bấm mục **Bài đăng** (mục đầu tiên của menu trái) | – | Về trang chủ /home (bảng tin); mục Bài đăng được tô đen | |
| G-09 | Khách | Gõ thẳng URL `{ctx}/order/list?as=buyer` | – | Chuyển /login, đăng nhập xong mở đúng Đơn mua | |
| G-10 | Khách | Gõ thẳng `{ctx}/account/profile` | – | Chuyển /login, đăng nhập xong mở đúng Hồ sơ cá nhân | |
| G-11 | Khách | Mở `{ctx}/users/profile?id=S1` (bấm tên người bán) | – | Chuyển /login | |
| G-12 | Khách | Bấm **Đăng nhập** ở góc phải khi đang ở trang tìm kiếm | – | Đăng nhập xong quay về đúng trang tìm kiếm (giữ từ khoá) | |
| G-13 | Khách | Bấm **Đăng ký** ở góc phải | – | Mở trang đăng ký | |
| G-14 | Đã đăng nhập B1 | Mở `{ctx}/login` hoặc `{ctx}/register` | – | Tự chuyển về /home | |
| G-15 | Đã đăng nhập A1 | Mở `{ctx}/login` | – | Tự chuyển về /admin/dashboard | |
| G-16 | Khách | Mở `{ctx}/product?id=<id bài đang chờ duyệt / đã ẩn>` | – | Báo 404 "Bài đăng không tồn tại hoặc đã bị gỡ" | |
| G-17 | Khách | Gọi `{ctx}/notifications/count` (fetch, không phải mở trang) | – | Trả 401 + JSON `{"error":"Vui lòng đăng nhập","login":...}` | |
| G-18 | Khách | Mở link có `?next=//evil.com` : `{ctx}/login?next=//evil.com` rồi đăng nhập | – | Đăng nhập xong về /home, **không** bị đưa ra trang ngoài | |
| G-19 | Khách, lần đầu mở web (URL có `;jsessionid=`) | Mở `{ctx}/` trên trình duyệt mới | – | Vẫn vào Trang chủ, không bị đẩy sang /login | |

## 2. Tài khoản – Đăng nhập, Đăng ký, Quên mật khẩu, Google/Facebook (F1)

| Mã TC | Tiền điều kiện | Các bước | Dữ liệu | Kết quả mong đợi | Kết quả |
|---|---|---|---|---|---|
| AC-01 | Khách | Đăng nhập đúng | b1@… / 123456 | Vào trang chủ (hoặc trang đang định mở), góc trái dưới hiện tên B1 | |
| AC-02 | Khách | Để trống email hoặc mật khẩu, bấm Đăng nhập | – | "Vui lòng nhập email và mật khẩu." | |
| AC-03 | Khách | Sai mật khẩu 1 lần | b1@… / 111111 | "Email hoặc mật khẩu không đúng. Bạn còn 4 lần thử." | |
| AC-04 | Khách | Sai mật khẩu 5 lần liên tiếp | – | Bị khoá: "Bạn đã nhập sai 5 lần. Vui lòng thử lại sau 5 phút." Đúng mật khẩu trong 5 phút vẫn bị chặn | |
| AC-05 | Khách | Email không tồn tại | x@x.com | Báo sai email hoặc mật khẩu (không tiết lộ email có tồn tại hay không) | |
| AC-06 | Tài khoản đã bị admin khoá | Đăng nhập | – | "Tài khoản của bạn đang bị khóa." | |
| AC-07 | Đang đăng nhập B1, admin khoá B1 | B1 mở /account/profile | – | Bị đăng xuất, về /login?banned=1 | |
| AC-08 | Đã đăng nhập | Cài đặt → **Đăng xuất** | – | Về trang khách, góc phải hiện Đăng nhập / Đăng ký | |
| AC-09 | Khách | Đăng ký với email mới, mật khẩu hợp lệ | Email thật của bạn, mật khẩu `abc12345` | Chuyển sang trang nhập mã 6 số; email nhận được mã (nếu chưa cấu hình mail thì mã in trong log Tomcat) | |
| AC-10 | Khách | Đăng ký với email đã có | b1@cunguoimoita.com | "Email này đã được sử dụng." | |
| AC-11 | Khách | Đăng ký, 2 ô mật khẩu khác nhau | – | "Mật khẩu nhập lại chưa khớp." | |
| AC-12 | Khách | Đăng ký mật khẩu yếu | `1234567`, `abcdefgh`, `abc 1234` | Báo cần ≥ 8 ký tự, có chữ và số, không khoảng trắng | |
| AC-13 | Khách | Đăng ký email sai định dạng | `abc@` | "Vui lòng nhập email hợp lệ." | |
| AC-14 | Đang ở bước nhập mã | Nhập đúng mã 6 số | – | Tạo tài khoản thành công, đăng nhập được | |
| AC-15 | Đang ở bước nhập mã | Nhập mã không đủ 6 số | `123` | "Mã xác nhận gồm 6 chữ số." | |
| AC-16 | Đang ở bước nhập mã | Nhập sai mã 5 lần | – | Báo đã nhập sai quá 5 lần, phải đăng ký / gửi lại mã | |
| AC-17 | Đang ở bước nhập mã | Bấm Gửi lại mã ngay lập tức | – | Báo phải chờ đủ 60 giây | |
| AC-18 | Đang ở bước nhập mã | Chờ quá 10 phút rồi nhập mã cũ | – | Báo mã hết hạn | |
| AC-19 | Khách | Quên mật khẩu → nhập email đã có | b1@… | Gửi mã 6 số, chuyển sang bước nhập mã | |
| AC-20 | Khách | Quên mật khẩu với email không có | x@x.com | "Không tìm thấy tài khoản nào dùng email này." | |
| AC-21 | Đã nhận mã quên mật khẩu | Nhập đúng mã → đặt mật khẩu mới hợp lệ | `newpass123` | Đổi xong, đăng nhập bằng mật khẩu mới được, mật khẩu cũ không được | |
| AC-22 | Đã nhận mã | Mật khẩu mới và nhập lại khác nhau | – | Báo chưa khớp | |
| AC-23 | Khách | Bấm **Google** | Tài khoản Google thật | Đăng nhập/đăng ký bằng Google, về trang đang định mở | |
| AC-24 | Khách | Bấm **Facebook**, tài khoản Facebook không có email | – | Báo không cung cấp email đã xác minh, "Hãy đăng ký bằng email." | |
| AC-25 | Khách | Huỷ ở màn hình đồng ý của Google | – | Quay lại /login kèm thông báo lỗi, không bị lỗi 500 | |

## 3. Hồ sơ cá nhân, Sổ địa chỉ, Đổi mật khẩu, Ảnh đại diện, Hạng thành viên (F1, F8)

| Mã TC | Tiền điều kiện | Các bước | Dữ liệu | Kết quả mong đợi | Kết quả |
|---|---|---|---|---|---|
| PR-01 | B1 | Cài đặt → Hồ sơ cá nhân | – | Trang nằm trong khung trắng viền đen; có thẻ **Hạng thành viên** ở trên, rồi Thông tin cá nhân, Đổi mật khẩu, Sổ địa chỉ | |
| PR-02 | B1 | Xem thẻ Hạng thành viên | – | Hiện hạng (Bronze/Silver/Gold/Diamond), số ecoPoints, ưu đãi, thanh tiến độ "Còn X điểm nữa để lên …", điểm đánh giá trung bình, số đơn đã bán, tối đa 5 đánh giá gần nhất | |
| PR-03 | B1 | Bấm nút "🌟 Hạng thành viên" đầu trang hồ sơ | – | Cuộn xuống thẻ Hạng thành viên | |
| PR-04 | B1 | Mở Cài đặt | – | Có: Hồ sơ cá nhân, Sổ địa chỉ, Đổi mật khẩu, **Lịch sử thanh toán**, Thu nhập bán hàng, Khiếu nại & hoàn tiền, Đăng xuất. **Không** có Hạng thành viên, Đơn mua, Đơn bán (Đơn mua/Đơn bán nằm ở menu trái) | |
| PR-05 | B1 | Mở thẳng `{ctx}/review/tier` | – | Trang hạng cũ vẫn mở được (trong khung chung) | |
| PR-06 | Tài khoản đạt 3000 điểm (Diamond) | Xem thẻ hạng | – | "Bạn đã đạt hạng cao nhất 🎉", không có thanh tiến độ | |
| PR-07 | B1 | Sửa họ tên, số điện thoại → Lưu | "Người mua Một", 0901234567 | "Hồ sơ đã được cập nhật."; tên góc trái dưới đổi theo | |
| PR-08 | B1 | Để trống họ tên → Lưu | – | "Vui lòng kiểm tra lại họ tên, số điện thoại và tài khoản nhận tiền." | |
| PR-09 | B1 | Số điện thoại sai | `abc`, `123` | Báo lỗi như PR-08 | |
| PR-10 | B1 | Tải ảnh đại diện PNG/JPEG < 5 MB | – | "Ảnh đại diện đã được cập nhật."; avatar đổi ở menu, bài đăng, chat | |
| PR-11 | B1 | Tải ảnh > 5 MB hoặc file .pdf | – | "Ảnh không hợp lệ hoặc vượt quá 5 MB…" | |
| PR-12 | B1 | Đổi mật khẩu đúng mật khẩu hiện tại | 123456 → `abc12345` | "Mật khẩu đã được đổi."; đăng nhập lại bằng mật khẩu mới được (đổi lại 123456 sau khi test) | |
| PR-13 | B1 | Sai mật khẩu hiện tại | – | "Mật khẩu hiện tại chưa đúng." | |
| PR-14 | B1 | Mật khẩu mới ≠ nhập lại | – | "Hai ô mật khẩu mới chưa trùng nhau." | |
| PR-15 | B1 | Mật khẩu mới yếu | `abcdefgh` | "Mật khẩu mới cần ít nhất 8 ký tự, có chữ cái và chữ số…" | |
| PR-16 | B1 | Thêm địa chỉ: chọn Tỉnh → Quận → Phường, nhập số nhà | – | "Sổ địa chỉ đã được cập nhật."; danh sách có địa chỉ mới | |
| PR-17 | B1 | Thêm địa chỉ thiếu tỉnh / số nhà | – | "Vui lòng kiểm tra lại thông tin địa chỉ." | |
| PR-18 | B1 có 2 địa chỉ | Đặt địa chỉ thứ 2 làm mặc định | – | Địa chỉ mặc định đổi, lên đầu danh sách | |
| PR-19 | B1 | Xoá / sửa một địa chỉ | – | Cập nhật đúng | |
| PR-20 | B1 | Gửi form sửa địa chỉ với id địa chỉ của B2 (sửa hidden input) | id địa chỉ `AD-B2` | "Không tìm thấy địa chỉ cần thao tác.", địa chỉ B2 không đổi | |
| PR-21 | A1 | Mở `{ctx}/account/profile` | – | Lỗi 403 (admin không có hồ sơ khách hàng) | |
| PR-22 | B1 | Mở `{ctx}/users/profile?id=S1` | – | Hồ sơ công khai của người bán S1: tên, đánh giá, bài đang bán | |

## 4. Đăng bài, Bài của tôi (F2)

| Mã TC | Tiền điều kiện | Các bước | Dữ liệu | Kết quả mong đợi | Kết quả |
|---|---|---|---|---|---|
| PD-01 | S1 | Đăng bài: loại **Bán**, đủ thông tin, 3 ảnh | Tên "Bàn phím test", giá 600000 | Lưu thành công; bài ở trạng thái **Chờ duyệt**; chưa hiện trên bảng tin | |
| PD-02 | S1 | Đăng bài không có ảnh | – | "Cần ít nhất 1 ảnh của món đồ" | |
| PD-03 | S1 | Tên ngắn hơn 3 ký tự / dài hơn 200 ký tự | "ab" | "Tên món đồ cần ít nhất 3 ký tự" / "Tên món đồ tối đa 200 ký tự" | |
| PD-04 | S1 | Không chọn danh mục / tình trạng / loại bài | – | "Hãy chọn danh mục" / "Hãy chọn tình trạng món đồ" / "Hãy chọn loại bài…" | |
| PD-05 | S1 | Giá âm, chữ, hoặc > 9.999.999.999 | -1, abc | "Số tiền phải từ 0 đến 9.999.999.999 đ" / "Số tiền không hợp lệ" | |
| PD-06 | S1 | Mô tả > 5000 ký tự; "Món muốn đổi" > 500 ký tự | – | Báo vượt giới hạn | |
| PD-07 | S1 | Ảnh > 5 MB hoặc tổng > 45 MB | – | "Mỗi ảnh tối đa 5 MB, tổng dung lượng tối đa 45 MB" | |
| PD-08 | S1 | Đăng bài loại **Trao đổi** | Giá trị ước tính, món muốn đổi | Form ẩn ô Giá bán, hiện ô giá trị ước tính & món muốn đổi | |
| PD-09 | S1 | Bỏ tick "Nhận COD" khi đăng bài Bán | – | Trang chi tiết bài ghi "Không nhận" COD | |
| PD-10 | S1 | Bài của tôi → lọc tab Tất cả / Đang ẩn / Đã bán-đổi | – | Đếm số và lọc đúng | |
| PD-11 | S1, bài đã duyệt | Bấm **Tạm ẩn bài** | – | Bài biến khỏi bảng tin; trang chi tiết với khách báo 404 | |
| PD-12 | S1, bài đang tạm ẩn | Bấm **Hiện lại bài** | – | Bài hiện lại trên bảng tin | |
| PD-13 | S1, bài bị admin từ chối | Sửa bài → Lưu | – | Bài quay về "Chờ duyệt" | |
| PD-14 | S1, bài đã có người đặt (reserved) | Bấm Sửa bài | – | "Bài đã có người đặt hoặc đã bị admin ẩn nên không sửa được" | |
| PD-15 | S1, bài đã có người nhắn tin / cho vào giỏ | Xoá bài | – | "Bài đã có người nhắn tin, cho vào giỏ hoặc đặt mua nên không xoá được, bạn có thể ẩn bài" | |
| PD-16 | S2 | Mở `{ctx}/product/edit?id=<bài của S1>` | – | Không sửa được ("Không tìm thấy bài đăng của bạn") | |
| PD-17 | A1 | Menu trái | – | Không có mục Đăng bài / Bài của tôi | |

## 5. Bảng tin, Tìm kiếm & lọc, Chi tiết bài, Đã lưu (F3)

| Mã TC | Tiền điều kiện | Các bước | Dữ liệu | Kết quả mong đợi | Kết quả |
|---|---|---|---|---|---|
| FD-01 | Có ≥ 1 bài đã duyệt | Mở trang chủ | – | Bài mới nhất ở trên; mỗi bài có avatar, tên người bán, danh mục, tình trạng, giá, tối đa 3 ảnh (+N nếu nhiều hơn) | |
| FD-02 | > 1 trang bài | Bấm "Xem thêm bài →" / "← Bài mới hơn" | – | Phân trang đúng | |
| FD-03 | – | Tìm theo từ khoá có dấu / không dấu | "ban phim", "Bàn phím" | Ra kết quả phù hợp | |
| FD-04 | – | Lọc theo danh mục, tình trạng (mới/đã dùng/hư), loại bài (bán/đổi) | – | Chỉ ra bài đúng điều kiện | |
| FD-05 | – | Lọc giá từ 1.000.000 đến 100.000 (min > max) | – | "Giá thấp nhất đang lớn hơn giá cao nhất, mình đã đổi chỗ hai giá" | |
| FD-06 | – | Giá lọc là chữ | abc | Bỏ lọc giá, vẫn hiện kết quả kèm thông báo lỗi | |
| FD-07 | – | Sắp xếp: mới nhất / giá tăng / giá giảm | – | Thứ tự đúng | |
| FD-08 | B1 | Mở chi tiết bài | – | Ảnh lớn + dải ảnh nhỏ (bấm đổi ảnh lớn), giá, tình trạng, COD, người đăng, mô tả, "Bài khác của người bán" | |
| FD-09 | B1, bài loại Trao đổi | Mở chi tiết | – | Có mục "Gợi ý trao đổi" (bài giá trị lệch ≤ 30%) | |
| FD-10 | S1 | Mở chi tiết bài của chính mình | – | Có nút Sửa bài / Tạm ẩn / Bài đăng của tôi, không có Thêm vào giỏ / Nhắn tin | |
| FD-11 | B1 | Bấm **Lưu** trên bảng tin | – | Nút đổi thành "Đã lưu" ngay, trang không tải lại | |
| FD-12 | B1 | Bấm "Đã lưu" lần nữa | – | Bỏ lưu | |
| FD-13 | B1 | Menu **Đã lưu** | – | Trang trong khung chung, liệt kê đúng các bài đã lưu, kể cả bài đã bán | |
| FD-14 | S1 | Lưu bài của chính mình (gửi form) | – | "Không cần lưu bài của chính bạn" | |
| FD-15 | Bài đã bán đang nằm trong Đã lưu | Mở Đã lưu | – | Bài vẫn hiện, có trạng thái đã bán | |
| FD-16 | B1 | Bấm "Báo cáo bài đăng vi phạm" | Lý do "Lừa đảo" | Gửi thành công (xem mục 13) | |

## 6. Giỏ hàng kiểu Shopee & Đặt hàng (F4)

| Mã TC | Tiền điều kiện | Các bước | Dữ liệu | Kết quả mong đợi | Kết quả |
|---|---|---|---|---|---|
| CT-01 | B1 | Mở Giỏ hàng | – | Trang trong khung chung; có 3 tab **Giỏ hàng / Đơn mua / Đơn bán**; menu trái cũng có mục Đơn mua và Đơn bán riêng | |
| CT-02 | B1, giỏ trống | Mở Giỏ hàng | – | "Giỏ hàng của bạn còn trống." + nút "Tiếp tục mua sắm" | |
| CT-03 | B1 | Thêm vào giỏ 2 bài của S1 và 2 bài của S2 | – | "Đã thêm vào giỏ"; giỏ chia 2 khối shop, mỗi khối có tên shop (bấm mở hồ sơ shop), ảnh nhỏ, tên, đơn giá, tình trạng, nút Xoá | |
| CT-04 | B1 | Thêm lại 1 bài đã có trong giỏ | – | "Sản phẩm đã có trong giỏ hàng" | |
| CT-05 | S1 | Thêm bài của chính mình vào giỏ (gửi form) | – | "Bạn không thể mua sản phẩm của chính mình" | |
| CT-06 | Giỏ như CT-03 | Mới mở trang, chưa tick gì | – | "Tổng cộng (0 sản phẩm): 0 đ"; nút **Mua hàng** và "Xoá mục đã chọn" bị mờ | |
| CT-07 | Giỏ như CT-03 | Tick 1 món | – | Tổng = giá món đó, "(1 sản phẩm)"; ô shop ở trạng thái nửa chọn (–) | |
| CT-08 | Giỏ như CT-03 | Tick ô của shop S1 | – | Tick hết món của S1; tổng = tổng các món S1 | |
| CT-09 | Giỏ như CT-03 | Tick **Chọn tất cả** (trên hoặc dưới) | – | Tick hết mọi món còn hàng; cả 2 ô Chọn tất cả cùng tick; "Chọn tất cả (N)" đúng số món mua được | |
| CT-10 | Giỏ như CT-09 | Bỏ tick 1 món | – | Ô Chọn tất cả chuyển nửa chọn; tổng trừ đúng | |
| CT-11 | 1 món trong giỏ đã bị B2 đặt trước (reserved) | Mở giỏ của B1 | – | Món đó mờ, gạch tên, ghi "Đã có người đặt", **không tick được**, không tính vào Chọn tất cả | |
| CT-12 | Giỏ như CT-03 | Tick 1 món của S1 → **Mua hàng** | – | Tạo 1 đơn, mở trang chi tiết đơn; các món không tick **vẫn còn trong giỏ** | |
| CT-13 | Giỏ như CT-03 | Tick món của cả S1 và S2 → Mua hàng | – | "Đã tạo 2 đơn (mỗi người bán 1 đơn)", mở Đơn mua | |
| CT-14 | B1 chưa có địa chỉ nào | Tick món → Mua hàng | – | "Bạn cần thêm địa chỉ giao hàng trước khi đặt hàng" | |
| CT-15 | B1 và B2 cùng có món X trong giỏ | B2 mua X trước, rồi B1 (trang chưa tải lại) tick X → Mua hàng | – | B1 nhận lỗi ""X" đã có người đặt. Hãy xoá món này khỏi giỏ rồi đặt lại."; không đơn nào được tạo cho B1 | |
| CT-16 | B1 | Gửi form Mua hàng không có món nào (dùng DevTools bỏ disabled) | – | "Vui lòng chọn sản phẩm để mua" | |
| CT-17 | B1 | Bấm **Xoá** ở 1 món | – | Món biến khỏi giỏ | |
| CT-18 | B1 | Tick 2 món → **Xoá mục đã chọn** | – | Xoá đúng 2 món đó, món khác còn nguyên | |
| CT-19 | B1 | Bấm Mua hàng 2 lần thật nhanh | – | Chỉ tạo đơn 1 lần (lần 2 báo món đã có người đặt hoặc giỏ trống) | |
| CT-20 | B1 | Thu nhỏ cửa sổ ≤ 684 px | – | Giỏ chuyển dạng điện thoại, thanh tổng tiền vẫn dính đáy, không tràn ngang | |
| CT-21 | B1, bài đang bán | Trang chi tiết bài → **Thêm vào giỏ** với bài đã có người đặt | – | Nút bị mờ; gửi form trực tiếp báo "Sản phẩm đã có người đặt hoặc ngừng bán" | |

## 7. Thanh toán đơn: địa chỉ → vận chuyển → voucher → COD / online (F4, F5, F6)

| Mã TC | Tiền điều kiện | Các bước | Dữ liệu | Kết quả mong đợi | Kết quả |
|---|---|---|---|---|---|
| OD-01 | Đơn mới của B1 | Mở chi tiết đơn | – | Khung chung; mục "Đặt hàng & thanh toán" gồm 4 bước: Địa chỉ, Đơn vị vận chuyển, Voucher, Thanh toán; **không có ô Huỷ đơn** khi đang tạo đơn | |
| OD-01b | OD-01 | Xem bảng Sản phẩm | – | Mỗi món có ảnh nhỏ + tên (bấm mở trang bài), **mã sản phẩm**, số lượng, đơn giá, thành tiền; dưới là Tiền hàng, Phí vận chuyển, Giảm giá, Phí dịch vụ, Tổng thanh toán | |
| OD-02 | OD-01 | Đổi địa chỉ giao → Lưu địa chỉ | Địa chỉ khác tỉnh | "Đã cập nhật đơn hàng"; nếu đã chọn hãng thì phí ship tính lại | |
| OD-03 | OD-01 | Chọn hãng GHN / GHTK / Ahamove / Viettel Post | – | Phí ship cộng vào Tổng thanh toán, hiện số ngày dự kiến | |
| OD-04 | Chưa chọn hãng | Xem bước 4 | – | "Chọn đơn vị vận chuyển trước để biết tổng tiền.", chưa có nút thanh toán | |
| OD-05 | Đơn ≥ 500.000đ | Nhập voucher `GIAM50K` → Áp dụng | – | Giảm 50.000đ; "Đã áp voucher ✔" | |
| OD-06 | Đơn < 500.000đ | Áp `GIAM50K` | – | Báo chưa đủ giá trị tối thiểu | |
| OD-07 | – | Áp `HETHAN20K` | – | "Voucher không còn hiệu lực" / "Voucher đã được sử dụng hoặc đã hết hạn" | |
| OD-08 | – | Áp mã không tồn tại | `ABC` | "Mã voucher không tồn tại" | |
| OD-09 | Đã áp 1 voucher | Áp thêm voucher khác (gửi form) | – | "Đơn này đã được áp dụng voucher" | |
| OD-10 | B2 | Áp voucher `GIAM50K` của B1 vào đơn của B2 | – | "Voucher này không thuộc quyền sở hữu của bạn" | |
| OD-11 | Đã chọn hãng | Bấm **Đặt hàng, trả tiền khi nhận (COD)** | – | Đơn giữ trạng thái Mới, vận đơn ghi số tiền thu hộ = tổng đơn; **mã vận đơn tự sinh** (VD GHN123456789); thanh trạng thái ở bước 1 "Chờ người bán xác nhận"; người bán nhận thông báo; các bước 1–4 không sửa được nữa | |
| OD-12 | Bài không nhận COD | Xem bước 4 | – | Không có nút COD, ghi "Người bán không nhận thanh toán khi nhận hàng (COD) cho sản phẩm này." | |
| OD-13 | Người mua đã từ chối nhận COD 2 lần | Xem bước 4 | – | "Bạn đã từ chối nhận hàng COD 2 lần nên không dùng được COD, vui lòng thanh toán online." | |
| OD-14 | Đã chọn hãng | **Thanh toán online →** | – | Đơn chuyển "Chờ thanh toán", sang trang chọn cổng: VNPay / MoMo / Chuyển khoản VietQR | |
| OD-15 | OD-14 | Chọn cổng giả lập → Thanh toán thành công | – | "Thanh toán thành công ✔"; đơn "Đã thanh toán"; **mã vận đơn tự sinh** theo hãng đã chọn (VD GHTK…); giao dịch hiện ở trang Lịch sử thanh toán | |
| OD-16 | OD-14 | Ở trang giả lập chọn thất bại / huỷ | – | Báo thất bại, đơn vẫn "Chờ thanh toán", có nút "Tiếp tục thanh toán online →" | |
| OD-17 | VNPay sandbox thật (nếu đã cấu hình vnpay.properties) | Thanh toán bằng thẻ test NCB | – | VNPay trả về /payment/vnpay-return, đơn thành Đã thanh toán | |
| OD-18 | B2 | Mở `{ctx}/payment/checkout?orderId=<đơn của B1>` | – | "Bạn không có quyền thanh toán đơn hàng này" | |
| OD-19 | B2 | Mở `{ctx}/order/detail?id=<đơn của B1>` | – | 404 | |
| OD-20 | Đơn đã đặt COD hoặc đã thanh toán, người bán chưa xác nhận giao | Người mua hoặc người bán nhập lý do → **Huỷ đơn** | – | Đơn Đã huỷ; sản phẩm trở lại "đang bán"; bên kia nhận thông báo; trang đơn đã huỷ chỉ còn dòng "Đơn đã huỷ.", không có khung trống ở cuối | |
| OD-21 | Đơn đã thanh toán online | Huỷ đơn | – | Nút ghi "Huỷ đơn và hoàn tiền"; tạo khoản hoàn tiền, người mua nhận thông báo "Đã hoàn … đ" | |
| OD-22 | – | Huỷ đơn không nhập lý do | – | "Vui lòng nhập lý do huỷ đơn" | |
| OD-23 | Người bán đã xác nhận giao hàng (Chuẩn bị vận chuyển trở đi) | Tìm ô Huỷ đơn (cả 2 bên) | – | Không còn ô Huỷ đơn | |
| OD-24 | Đơn chờ thanh toán online (người mua chưa trả) | B1 mở chi tiết đơn | – | Vẫn ở bước đặt hàng, không có ô Huỷ đơn; người bán (S1) thấy "Chờ người mua chọn vận chuyển và thanh toán." và **có** ô Huỷ đơn | |
| OD-25 | B1 có giao dịch online | Cài đặt → **Lịch sử thanh toán** | – | Trang riêng trong khung chung: bảng Mã giao dịch, Đơn hàng (bấm mở đơn), Cổng, Số tiền, Trạng thái, Thời gian; trang chi tiết đơn không còn bảng lịch sử thanh toán (chỉ có link) | |
| OD-26 | B1 chưa thanh toán online lần nào (VD B2) | Mở Lịch sử thanh toán | – | "Bạn chưa có giao dịch thanh toán nào." | |

## 8. Mua ngay (trang chi tiết bài & khung chat)

| Mã TC | Tiền điều kiện | Các bước | Dữ liệu | Kết quả mong đợi | Kết quả |
|---|---|---|---|---|---|
| BN-01 | B1 đang chat với S1 về bài đang bán | Mở Tin nhắn → đoạn chat | – | Khung món đầu đoạn chat có **Trả giá món này** và **Mua ngay** cạnh nhau | |
| BN-02 | BN-01 | Bấm **Mua ngay** | – | Tạo đơn 1 món với **đúng giá niêm yết** người bán đặt; mở trang chi tiết đơn | |
| BN-03 | BN-02 xong | Mở Giỏ hàng | – | Món vừa mua không còn trong giỏ; các món khác giữ nguyên | |
| BN-04 | S1 mở cùng đoạn chat (phía người bán) | Xem khung món | – | Không có Mua ngay, không có Trả giá | |
| BN-05 | Bài đã có người đặt / đã bán | B1 mở đoạn chat | – | Không có nút Mua ngay | |
| BN-06 | Bài loại chỉ **Trao đổi** | B1 mở đoạn chat | – | Không có nút Mua ngay | |
| BN-07 | B1 | Bấm Mua ngay ở **khung chat nổi** (góc phải dưới) | – | Giống BN-02 | |
| BN-08 | Bài vừa bị B2 mua trước khi B1 bấm | B1 bấm Mua ngay (trang chưa tải lại) | – | Quay lại giỏ với lỗi "Sản phẩm đã có người đặt hoặc ngừng bán" | |
| BN-09 | B1 chưa có địa chỉ | Bấm Mua ngay | – | "Bạn cần thêm địa chỉ giao hàng trước khi đặt hàng" | |

## 9. Vận chuyển, nhận hàng, trả hàng/hoàn tiền (F6, F9)

| Mã TC | Tiền điều kiện | Các bước | Dữ liệu | Kết quả mong đợi | Kết quả |
|---|---|---|---|---|---|
| SH-01 | Đơn chưa thanh toán / chưa xác nhận COD | S1 mở chi tiết đơn bán | – | "Chờ người mua chọn vận chuyển và thanh toán."; không có mục Xác nhận giao hàng; **không** thấy các bước đặt hàng của người mua | |
| SH-02 | Đơn đã trả online hoặc đã đặt COD | S1 mở chi tiết đơn bán → mục **Xác nhận giao hàng** → chọn **Mang đến bưu cục gần nhất** → Xác nhận | – | "Đã xác nhận giao hàng: người bán mang hàng đến bưu cục gần nhất"; vận đơn "Chuẩn bị vận chuyển"; đơn "Đang giao"; B1 nhận thông báo; thanh trạng thái của B1 sang bước 2 | |
| SH-03 | Như SH-02, S1 có địa chỉ trong sổ | Chọn **Nhân viên đến lấy hàng** → chọn địa chỉ → Xác nhận | Địa chỉ AD-S1 | "Đã xác nhận giao hàng: nhân viên vận chuyển sẽ đến lấy hàng tại …" | |
| SH-03b | Như SH-02 | Gửi `handover=pickup` với id địa chỉ của người khác (DevTools) | addressId=AD-B1 | "Vui lòng chọn địa chỉ lấy hàng trong sổ địa chỉ của bạn" | |
| SH-03c | Người bán chưa có địa chỉ nào | Xem mục Xác nhận giao hàng | – | Lựa chọn "Nhân viên đến lấy hàng" bị mờ, có link "Thêm địa chỉ" | |
| SH-03d | Như SH-02 | B1 (người mua) gửi `action=handover` tới /shipping/track | – | "Bạn không có quyền với đơn hàng này" | |
| SH-03e | Đơn chưa thanh toán | S1 gửi `action=handover` (DevTools) | – | "Người mua chưa thanh toán hoặc chưa xác nhận COD, chưa giao hàng được" | |
| SH-04 | Chuẩn bị vận chuyển | S1 → Xem vận chuyển → "Giả lập: đang giao" | – | Dòng thời gian tô đậm tới "Đang vận chuyển"; B1 thấy bước 3 | |
| SH-05 | Đang giao, đơn COD | Bấm "Giả lập: đã giao" khi shipper chưa thu tiền | – | "Đơn COD phải ghi nhận shipper đã thu tiền trước khi báo đã giao" | |
| SH-06 | Đang vận chuyển | (COD: bấm "Shipper đã thu tiền COD" trước) → "Giả lập: đã giao" | – | Vận đơn "Đã giao thành công" + ngày giao; đơn "Đã giao"; B1 thấy bước 4 | |
| SH-07 | S1 (người bán) | Xem trang theo dõi vận chuyển | – | **Không** có nút "Giả lập: hoàn hàng" | |
| SH-08 | S1 | Gửi POST `action=status&status=returned` (DevTools) | – | "Người bán không thể tự hoàn hàng" | |
| SH-09 | A1 | Mở trang theo dõi của đơn đang giao | – | Có nút "Giả lập: hoàn hàng" | |
| SH-10 | B1 | Mở trang theo dõi | – | Chỉ xem, không có nút nào | |
| SH-11 | Đơn **Đang giao** (bước 2–3) | B1 mở chi tiết đơn | – | Thanh trạng thái 4 bước tô tới bước hiện tại; có nút "Xem vận chuyển →"; **không** có "Đã nhận hàng" / "Trả hàng/Hoàn tiền" | |
| SH-12 | Đơn **Đã giao** | B1 mở chi tiết đơn | – | Hiện 2 nút **Đã nhận hàng** và **Trả hàng/Hoàn tiền** + dòng "tự hoàn tất sau 7 ngày" | |
| SH-13 | Đơn Đã giao | B1 bấm **Đã nhận hàng** | – | Đơn "Hoàn tất"; sản phẩm "đã bán"; S1 nhận thông báo; tạo khoản thu nhập cho S1 | |
| SH-14 | Đơn Đang giao | B1 gửi `action=received` (DevTools) | – | "Chỉ xác nhận đã nhận hàng được sau khi đơn đã giao" | |
| SH-15 | Đơn Đã giao | S1 (người bán) gửi `action=received` | – | "Chỉ người mua mới làm được thao tác này" | |
| SH-16 | Đơn Đã giao | S1 mở chi tiết đơn bán | – | Không có nút Đã nhận hàng / Trả hàng/Hoàn tiền | |
| SH-17 | Đơn Đã giao, không ai bấm gì | Đợi 7 ngày (hoặc A1 bấm chạy tác vụ tự hoàn tất ở Dashboard) | – | Đơn tự "Hoàn tất"; B1 nhận "…Hãy đánh giá người bán nhé!" | |
| SH-18 | Đơn Đã giao | Danh sách Đơn mua của B1 | – | Có link "Trả hàng/Hoàn tiền" ở cột Thao tác (chỉ đơn Đã giao) | |
| SH-19 | – | Mở `{ctx}/shipping/carriers` | – | Bảng 4 đơn vị vận chuyển, phí cơ bản, số ngày | |

## 10. Khiếu nại & hoàn tiền (F9)

| Mã TC | Tiền điều kiện | Các bước | Dữ liệu | Kết quả mong đợi | Kết quả |
|---|---|---|---|---|---|
| DP-01 | Đơn Đã giao | B1 bấm **Trả hàng/Hoàn tiền** → chọn lý do, mô tả, 2 ảnh → Gửi | "Hàng bị bể vỡ" | Mở trang chi tiết khiếu nại; S1 nhận thông báo; trên đơn hiện "Khiếu nại: Đang mở" | |
| DP-02 | – | Gửi khiếu nại không có mô tả | – | "Vui lòng mô tả vấn đề của đơn hàng" | |
| DP-03 | Đơn Đang giao | B1 mở `{ctx}/dispute?orderId=<id>` và gửi | – | "Chỉ yêu cầu trả hàng/hoàn tiền được sau khi đơn đã giao" | |
| DP-04 | Đơn đã có khiếu nại đang mở | Mở khiếu nại lần 2 | – | "Đơn này đang có khiếu nại chờ xử lý" | |
| DP-05 | – | S1 (người bán) mở khiếu nại trên đơn bán của mình | – | "Chỉ người mua của đơn mới mở khiếu nại được" | |
| DP-06 | Khiếu nại đang mở | B1 mở chi tiết đơn | – | Không còn nút Đã nhận hàng; gửi `received` báo "Đơn đang có khiếu nại, vui lòng chờ admin xử lý"; đơn không tự hoàn tất | |
| DP-07 | DP-01 | S1 gửi phản hồi + ảnh bằng chứng | – | Phản hồi hiện trên trang khiếu nại | |
| DP-08 | DP-01 | B1 nộp thêm ảnh | – | Ảnh thêm vào khiếu nại; không chọn ảnh thì "Vui lòng chọn ít nhất 1 ảnh" | |
| DP-09 | DP-01 | B2 mở `{ctx}/dispute?id=<id khiếu nại>` | – | "Không tìm thấy khiếu nại" | |
| DP-10 | DP-01 | A1 → Quản trị → Khiếu nại → **Xử cho người mua** (có lời giải thích) | – | Tạo khoản hoàn tiền; đơn "Đã hoàn tiền"; 2 bên nhận thông báo | |
| DP-11 | Khiếu nại mở khác | A1 **Xử cho người bán** | – | Đơn "Hoàn tất", tạo thu nhập cho người bán | |
| DP-12 | Khiếu nại mở khác | A1 **Từ chối** khiếu nại | – | Khiếu nại đóng | |
| DP-13 | – | A1 xử lý mà không nhập lời giải thích | – | "Vui lòng nhập lời giải thích" | |
| DP-14 | Khiếu nại đã xử lý | A1 xử lý lại | – | "Khiếu nại đã được xử lý xong" | |
| DP-15 | B1 | Cài đặt → Khiếu nại & hoàn tiền | – | Trang trong khung chung, không còn hàng tab cũ; danh sách khiếu nại (Tôi khiếu nại / Bị khiếu nại) và các khoản hoàn tiền | |

## 11. Thu nhập bán hàng (F9)

| Mã TC | Tiền điều kiện | Các bước | Dữ liệu | Kết quả mong đợi | Kết quả |
|---|---|---|---|---|---|
| PO-01 | S1 có đơn Hoàn tất | Cài đặt → **Thu nhập bán hàng** | – | Trang trong khung chung, tiêu đề "Thu nhập bán hàng"; liệt kê từng khoản: tiền hàng − phí sàn = thực nhận | |
| PO-02 | S1 hạng Bronze | Xem phí sàn | Đơn 2.000.000đ | Phí sàn 5% = 100.000đ | |
| PO-03 | Người bán hạng Silver / Gold / Diamond | Xem phí sàn | – | Phí giảm 1% / 3% / 5% so với 5% (ví dụ Silver: 5% × 0,99) | |
| PO-04 | S1 | Bấm xuất file (export) | – | Tải được file danh sách thu nhập | |
| PO-05 | S1 | Mở chi tiết đơn bán đã hoàn tất | – | Dòng "Tiền bán hàng: … − phí sàn … = …" + link Thu nhập của tôi | |
| PO-06 | B1 (không bán gì) | Mở Thu nhập bán hàng | – | Danh sách trống, không lỗi | |

## 12. Đánh giá & hạng thành viên (F8)

| Mã TC | Tiền điều kiện | Các bước | Dữ liệu | Kết quả mong đợi | Kết quả |
|---|---|---|---|---|---|
| RV-01 | Đơn Hoàn tất | B1 (người mua) mở chi tiết đơn | – | Có nút **Đánh giá giao dịch** | |
| RV-02 | RV-01 | Chọn 5 sao, viết nhận xét → Gửi | – | "Đã gửi đánh giá"; S1 nhận thông báo "Bạn vừa nhận được đánh giá 5 sao"; nút đánh giá biến mất | |
| RV-03 | RV-01 | Gửi không chọn sao | – | "Vui lòng chọn số sao" | |
| RV-04 | Đã đánh giá | Mở lại `{ctx}/review/create?orderId=<id>` | – | Về trang đơn với "Đơn này chưa thể đánh giá hoặc bạn đã đánh giá rồi" | |
| RV-05 | Đơn Hoàn tất | S1 (người bán) mở chi tiết đơn bán (Giỏ hàng → Đơn bán → Xem) | – | **Không** có thanh "Đánh giá giao dịch" | |
| RV-06 | Đơn Hoàn tất | S1 mở thẳng `{ctx}/review/create?orderId=<id>` | – | Bị trả về trang đơn kèm lỗi | |
| RV-07 | Đơn Hoàn tất | S1 gửi POST /review/create (DevTools) | rating=5 | "Chỉ người mua mới đánh giá được giao dịch" | |
| RV-08 | Đơn chưa hoàn tất | B1 mở /review/create | – | Không đánh giá được | |
| RV-09 | B2 (không thuộc đơn) | Gửi đánh giá đơn của B1 | – | Bị từ chối | |
| RV-10 | Đơn vừa hoàn tất | Xem hạng của B1 và S1 | – | ecoPoints cộng thêm (10.000đ giao dịch = 1 điểm); đủ 500 / 1000 / 3000 điểm thì lên Silver / Gold / Diamond | |
| RV-11 | S1 | Hồ sơ → thẻ Hạng thành viên | – | Đánh giá B1 vừa gửi hiện trong danh sách, điểm trung bình cập nhật | |

## 13. Tin nhắn, trả giá, ảnh, khung chat nổi (F7)

| Mã TC | Tiền điều kiện | Các bước | Dữ liệu | Kết quả mong đợi | Kết quả |
|---|---|---|---|---|---|
| CH-01 | B1 | Bài của S1 → **Nhắn tin, trả giá** | – | Mở đoạn chat mới; khung món ở đầu đoạn chat (ảnh, tên, giá đăng, tình trạng) | |
| CH-02 | CH-01 | Gửi tin nhắn chữ | "Còn hàng không ạ?" | Tin hiện ngay bên phải; S1 thấy số đỏ ở mục Tin nhắn và khung chat nổi | |
| CH-03 | CH-01 | Gửi tin trống | – | Không gửi ("Tin nhắn trống") | |
| CH-04 | CH-01 | Gửi ảnh (biểu tượng ảnh) | Ảnh ≤ 5 MB | Ảnh hiện trong đoạn chat, bấm mở ảnh lớn | |
| CH-05 | S1 | Bấm Nhắn tin trên bài của chính mình | – | "Bạn không thể nhắn tin với chính mình" | |
| CH-06 | CH-01 | B1 bấm **Trả giá món này**, nhập giá | 1.500.000 | Khung giá "Trả giá … đ – Chờ trả lời" | |
| CH-07 | CH-06 | Trả giá 0 hoặc chữ | 0 | "Giá trả phải lớn hơn 0" | |
| CH-08 | CH-06 đang chờ | B1 gửi thêm 1 giá nữa | – | "Đang có một offer chờ trả lời, hãy chấp nhận, từ chối hoặc trả giá lại offer đó" | |
| CH-09 | CH-06 | B1 tự bấm chấp nhận offer của mình (gửi request) | – | "Bạn không thể tự trả lời offer của mình" | |
| CH-10 | CH-06 | S1 bấm **Trả giá lại** | 1.600.000 | Offer cũ "Đã trả giá lại", offer mới chờ B1 | |
| CH-11 | CH-06 | S1 bấm **Từ chối** | – | Offer "Đã từ chối" | |
| CH-12 | CH-06 | S1 bấm **Chấp nhận** | – | Tạo đơn với giá đã chốt, sản phẩm "đã có người đặt"; khung giá có nút "Xem đơn" | |
| CH-13 | Món đã chốt với B1 | B2 trả giá cùng món | – | "Món này đã được chốt với người khác hoặc đã ngừng bán" | |
| CH-14 | Món đã bán | Mở đoạn chat | – | "Món này đã bán, đoạn chat đã đóng."; không gửi tin được | |
| CH-15 | B2 | Mở `{ctx}/chat/conversation?id=<đoạn chat B1–S1>` | – | Không xem được ("Không tìm thấy cuộc trò chuyện") | |
| CH-16 | B1 | Bấm biểu tượng chat nổi góc phải dưới | – | Hiện danh sách đoạn chat; bấm 1 đoạn mở cửa sổ nhỏ; thu nhỏ / đóng / mở lại sau khi tải trang vẫn giữ | |
| CH-17 | B1 | Trong cửa sổ chat nổi: gửi tin, trả giá, gửi ảnh | – | Hoạt động như trang Tin nhắn | |
| CH-18 | – | Mở trang Tin nhắn | – | Trang trong khung chung, 2 cột; khung chat nổi bị ẩn ở trang này | |
| CH-19 | B1 / S1 | Bấm **Báo cáo** trên đầu đoạn chat | – | Người mua báo cáo bài; người bán báo cáo người hỏi | |

## 14. Thông báo (F7)

| Mã TC | Tiền điều kiện | Các bước | Dữ liệu | Kết quả mong đợi | Kết quả |
|---|---|---|---|---|---|
| NT-01 | Có thông báo chưa đọc | Nhìn nút Thông báo | – | Số đỏ đúng số chưa đọc (> 9 hiện "9+"); tự cập nhật khoảng 20 giây/lần | |
| NT-02 | – | Bấm nút Thông báo | – | Danh sách xổ xuống; dòng chưa đọc in đậm, có chấm đỏ | |
| NT-03 | – | Bấm 1 thông báo | – | Mở đúng trang liên quan (đơn, khiếu nại, hạng…); thông báo thành đã đọc | |
| NT-04 | – | Nút "⋯" → Đánh dấu đã đọc | – | Dòng hết in đậm, số đỏ giảm | |
| NT-05 | – | "Đánh dấu tất cả đã đọc" | – | Số đỏ biến mất | |
| NT-06 | – | "Xem tất cả" | – | Trang /notifications trong khung chung | |
| NT-07 | B1 | Mở thông báo của B2 bằng id (`/notifications/open?id=…`) | – | Không mở được | |

## 15. Báo cáo vi phạm (F10)

| Mã TC | Tiền điều kiện | Các bước | Dữ liệu | Kết quả mong đợi | Kết quả |
|---|---|---|---|---|---|
| RP-01 | B1 | Báo cáo bài của S1, chọn lý do, mô tả | "Hàng giả" | Gửi thành công | |
| RP-02 | RP-01 | Báo cáo lại cùng bài | – | "Bạn đã báo cáo mục này, admin đang xem xét" | |
| RP-03 | B1 | Gửi báo cáo không chọn lý do | – | "Chưa chọn lý do báo cáo" | |
| RP-04 | B1 | Báo cáo chính mình (id = B1) | – | "Bạn không thể báo cáo chính mình" | |

## 16. Quản trị (F10) – đăng nhập A1

| Mã TC | Tiền điều kiện | Các bước | Dữ liệu | Kết quả mong đợi | Kết quả |
|---|---|---|---|---|---|
| AD-01 | B1 | Mở `{ctx}/admin/dashboard` | – | Bị chặn (403 / chuyển trang), không xem được | |
| AD-02 | Khách | Mở `{ctx}/admin/users` | – | Chuyển /login | |
| AD-03 | A1 | Dashboard | – | Số liệu: người dùng, bài chờ duyệt, báo cáo chờ, đơn, khiếu nại mở, đơn theo trạng thái; danh sách việc cần làm | |
| AD-04 | A1 | Dashboard → chạy ngay tác vụ tự hoàn tất đơn | – | Báo số đơn đã hoàn tất | |
| AD-05 | Bài chờ duyệt | Bài đăng → **Duyệt** | – | Bài hiện trên bảng tin | |
| AD-06 | Bài chờ duyệt | **Từ chối** | – | Bài "bị từ chối", người bán thấy trong Bài của tôi | |
| AD-07 | Bài đang bán | **Ẩn** (vi phạm) rồi **Hiện** lại | – | Bài biến mất rồi xuất hiện lại trên bảng tin | |
| AD-08 | Bài không ở trạng thái chờ duyệt | Duyệt lại (gửi request) | – | "Bài này không ở trạng thái chờ duyệt" | |
| AD-09 | A1 | Người dùng → **Khoá** B2 | – | B2 không đăng nhập được; nếu đang đăng nhập thì bị đẩy ra ở lần tải trang /account kế tiếp | |
| AD-10 | B2 bị khoá | **Mở khoá** | – | B2 đăng nhập lại được | |
| AD-11 | A1 | Khoá tài khoản admin | A1 | "Không thể khoá tài khoản admin" | |
| AD-12 | A1 | Danh mục: thêm danh mục con của "Điện tử" | – | Danh mục mới hiện trong form đăng bài | |
| AD-13 | A1 | Đặt danh mục cha là chính nó / cha là danh mục con | – | "Danh mục không thể là cha của chính nó" / "Danh mục cha phải là danh mục gốc (chỉ hỗ trợ 2 cấp)" | |
| AD-14 | A1 | Voucher: tạo mã mới cho B1 | `TEST-10K`, giảm 10000, đơn từ 0 | Tạo thành công; B1 thấy gợi ý voucher ở trang đơn | |
| AD-15 | A1 | Tạo voucher mã sai định dạng | `ab`, `giam 10k` | "Mã voucher gồm 3–30 chữ in hoa, số hoặc dấu gạch ngang" | |
| AD-16 | A1 | Voucher giảm ≤ 0 hoặc ngày hết hạn đã qua | – | "Số tiền giảm phải lớn hơn 0…" / "Ngày hết hạn đã qua" | |
| AD-17 | A1 | Voucher cho email không phải khách hàng | admin@… | "Không có khách hàng nào dùng email …" | |
| AD-18 | A1 | Hạng thành viên: sửa ngưỡng điểm, % giảm phí | – | Lưu được; ngưỡng phải tăng dần theo hạng | |
| AD-19 | A1 | Đơn vị vận chuyển: tắt 1 hãng | Ahamove | Hãng đó không còn trong bước chọn vận chuyển của đơn mới | |
| AD-20 | A1 | Báo cáo: xem báo cáo RP-01 → xử lý (ẩn bài / bỏ qua) | – | Báo cáo chuyển đã xử lý; xử lý lần 2 báo "Báo cáo này đã được xử lý" | |
| AD-21 | A1 | Khiếu nại: xem danh sách đang chờ, xử lý (xem mục 10) | – | Như DP-10 → DP-14 | |
| AD-22 | A1 | Menu trái | – | Có mục Quản trị; không có Đăng bài, Bài của tôi; Cài đặt chỉ có "Trang quản trị" và Đăng xuất | |

## 17. Giao diện chung (khung trang, tab, cỡ màn hình)

| Mã TC | Tiền điều kiện | Các bước | Dữ liệu | Kết quả mong đợi | Kết quả |
|---|---|---|---|---|---|
| UI-01 | B1 / S1 | Lần lượt mở: Giỏ hàng, Đơn mua, Đơn bán, Chi tiết đơn, Hồ sơ cá nhân, Bài của tôi, Đăng bài, Đã lưu, Tin nhắn, Thông báo, Thu nhập bán hàng, Khiếu nại & hoàn tiền, Hạng thành viên (/review/tier), Thanh toán, Theo dõi vận chuyển, Đánh giá | – | Mọi trang đều nằm trong **một khung trắng viền đen, bóng đen lệch**, tiêu đề font Bungee, chữ thường font Poppins | |
| UI-02 | – | Mở Giỏ hàng, Đơn mua, Đơn bán | – | Có 3 tab; tab của trang đang mở được tô đen; mục tương ứng ở menu trái cũng tô đen | |
| UI-03 | – | Mở Thu nhập bán hàng, Khiếu nại, Hạng thành viên | – | Không còn hàng 6 tab cũ | |
| UI-08 | B1 | Nhìn menu trái | – | Thứ tự: Bài đăng, Giỏ hàng, Đơn mua, Đơn bán, Tin nhắn, Đã lưu, Đăng bài, Bài của tôi, Cài đặt; icon nét đen; mục đang mở tô đen | |
| UI-04 | – | Nút "← Quay lại" đầu trang | – | Về trang trước trong web; mở thẳng link thì về trang cha | |
| UI-05 | – | Cửa sổ ≤ 1035 px | – | Menu trái chỉ còn icon | |
| UI-06 | – | Cửa sổ ≤ 684 px (điện thoại) | – | Ẩn menu trái; khung trang co lại, không có thanh cuộn ngang | |
| UI-07 | – | Sau khi cập nhật code, mở trang | – | CSS mới được tải (link CSS có `?v=20261020d`); nếu không, Ctrl+F5 | |

---

## Tổng hợp số testcase

| Phần | Số TC |
|---|---|
| 1. Chế độ khách | 20 |
| 2. Đăng nhập / Đăng ký / Quên mật khẩu / OAuth | 25 |
| 3. Hồ sơ, địa chỉ, mật khẩu, avatar, hạng | 22 |
| 4. Đăng bài, Bài của tôi | 17 |
| 5. Bảng tin, tìm kiếm, chi tiết, đã lưu | 16 |
| 6. Giỏ hàng & đặt hàng | 21 |
| 7. Thanh toán đơn | 27 |
| 8. Mua ngay | 9 |
| 9. Vận chuyển, nhận hàng, trả hàng | 23 |
| 10. Khiếu nại & hoàn tiền | 15 |
| 11. Thu nhập bán hàng | 6 |
| 12. Đánh giá & hạng | 11 |
| 13. Tin nhắn & trả giá | 19 |
| 14. Thông báo | 7 |
| 15. Báo cáo vi phạm | 4 |
| 16. Quản trị | 22 |
| 17. Giao diện chung | 8 |
| **Tổng** | **272** |
