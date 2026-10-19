// Chọn địa chỉ 2 cấp khi thêm/sửa địa chỉ (form có data-address-picker): chọn Tỉnh/Thành phố → Phường/Xã.
// Từ 01/07/2025 cả nước bỏ cấp quận/huyện, còn 34 tỉnh/thành và 3.321 phường/xã/đặc khu
// (Nghị quyết 202/2025/QH15 và các nghị quyết sắp xếp đơn vị cấp xã của UBTVQH).
// Dữ liệu: assets/data/dia-gioi-hanh-chinh.json, đóng gói sẵn trong web (không gọi API bên ngoài lúc chạy).
//   Nguồn: gói npm vietnam-address-database 1.0.0 (mã và tên theo danh mục của Tổng cục Thống kê),
//   đã đối chiếu số lượng với gói vietnam-address-data; rút gọn còn tên: [[tỉnh, [phường/xã, ...]], ...].
// Ô nào JS không chạy được (lỗi tải dữ liệu) thì vẫn là ô gõ tay như cũ. Giá trị đã lưu không có trong danh sách
// (địa chỉ cũ trước sáp nhập) vẫn được giữ và hiện chữ "(địa chỉ cũ)" để người dùng chọn lại.
(function () {
  const forms = document.querySelectorAll('form[data-address-picker]');
  if (!forms.length) return;
  const ctx = document.body.dataset.ctx || '';

  // So khớp tên không phân biệt hoa thường và bỏ chữ "Thành phố", "Phường"... ở đầu
  const PREFIX = /^(thành phố|tp\.?|tỉnh|phường|xã|đặc khu|thị trấn)\s+/;
  const key = name => (name || '').trim().toLowerCase().replace(PREFIX, '');
  // Xếp theo bảng chữ cái tiếng Việt, bỏ qua chữ "Thành phố"/"Phường"/"Xã" ở đầu để dễ tìm
  const byName = (a, b) => key(a).localeCompare(key(b), 'vi');

  function toSelect(input, placeholder) {
    const select = document.createElement('select');
    select.name = input.name;
    select.required = input.required;
    select.dataset.saved = input.value.trim();
    select.dataset.placeholder = placeholder;
    input.replaceWith(select);
    return select;
  }

  // Đổ danh sách vào ô chọn, chọn sẵn giá trị đã lưu (nếu có)
  function fill(select, names, saved) {
    select.replaceChildren(new Option(select.dataset.placeholder, ''));
    let found = false;
    names.forEach(name => {
      const opt = new Option(name, name);
      if (saved && key(name) === key(saved)) { opt.selected = true; found = true; }
      select.append(opt);
    });
    if (saved && !found) select.append(new Option(saved + ' (địa chỉ cũ)', saved, true, true));
    select.disabled = names.length === 0 && !saved;
  }

  function setup(form, data) {
    const provinceInput = form.querySelector('input[name="province"]');
    const wardInput = form.querySelector('input[name="ward"]');
    if (!provinceInput || !wardInput) return;
    const province = toSelect(provinceInput, 'Chọn tỉnh/thành phố');
    const ward = toSelect(wardInput, 'Chọn phường/xã');
    ward.required = true;

    const findProvince = () => data.find(p => key(p[0]) === key(province.value));
    const fillWards = saved => fill(ward, (findProvince() || [])[1] || [], saved);

    fill(province, data.map(p => p[0]), province.dataset.saved);
    fillWards(ward.dataset.saved);
    // Đổi tỉnh thì chọn lại phường/xã
    province.addEventListener('change', () => fillWards(''));
  }

  fetch(ctx + '/assets/data/dia-gioi-hanh-chinh.json')
    .then(res => { if (!res.ok) throw new Error(res.status); return res.json(); })
    .then(data => {
      data.sort((a, b) => byName(a[0], b[0]));
      data.forEach(p => p[1].sort(byName));
      forms.forEach(form => setup(form, data));
    })
    .catch(() => { /* không tải được dữ liệu: giữ ô gõ tay */ });
})();
