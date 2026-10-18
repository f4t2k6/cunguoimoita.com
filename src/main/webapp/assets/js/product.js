// Bảng tin, chi tiết bài, form đăng bài (F2, F3). Nạp ở các trang của hai chức năng này.
// Trang vẫn dùng được khi tắt JavaScript: mọi nút đều là form gửi bình thường.
(function () {
  // ---- Nút Lưu / Đã lưu: gửi bằng fetch để không tải lại trang (khỏi mất chỗ đang cuộn) ----
  document.addEventListener('submit', async (e) => {
    const form = e.target.closest('form.js-save');
    if (!form) return;
    e.preventDefault();
    const button = form.querySelector('button');
    const action = form.elements.action;
    button.disabled = true;
    try {
      const res = await fetch(form.action, {
        method: 'POST',
        headers: { 'X-Requested-With': 'fetch', Accept: 'application/json' },
        body: new URLSearchParams(new FormData(form))
      });
      const json = await res.json();
      if (res.status === 401 && json.login) { location.href = json.login; return; }
      if (!res.ok) { toast(json.error || 'Không lưu được, thử lại sau'); return; }
      // Cập nhật mọi nút Lưu của cùng một bài trên trang
      const id = form.elements.productId.value;
      document.querySelectorAll('form.js-save').forEach((f) => {
        if (f.elements.productId.value !== id) return;
        const b = f.querySelector('button');
        f.elements.action.value = json.saved ? 'remove' : 'save';
        b.textContent = json.saved ? 'Đã lưu' : 'Lưu';
        b.classList.toggle('is-saved', json.saved);
        b.setAttribute('aria-pressed', String(json.saved));
      });
      toast(json.saved ? 'Đã lưu vào Bucket List' : 'Đã bỏ lưu');
    } catch (err) {
      form.submit();   // mất kết nối fetch thì gửi form kiểu thường
    } finally {
      button.disabled = false;
    }
  });

  // ---- Hỏi lại trước khi xoá ----
  document.addEventListener('submit', (e) => {
    const form = e.target.closest('form.js-confirm');
    if (form && !window.confirm(form.dataset.confirm)) e.preventDefault();
  });

  // ---- Thông báo nhỏ góc dưới màn hình ----
  let toastTimer;
  function toast(text) {
    let el = document.getElementById('pdToast');
    if (!el) {
      el = document.createElement('div');
      el.id = 'pdToast';
      el.className = 'pd-toast';
      el.setAttribute('role', 'status');
      document.body.appendChild(el);
    }
    el.textContent = text;
    el.classList.add('show');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => el.classList.remove('show'), 2200);
  }

  // ---- Trang chi tiết: bấm ảnh nhỏ để đổi ảnh lớn ----
  const mainPhoto = document.getElementById('mainPhoto');
  document.querySelectorAll('.detail-thumbs .thumb').forEach((thumb) => {
    thumb.addEventListener('click', () => {
      mainPhoto.src = thumb.dataset.src;
      document.querySelectorAll('.detail-thumbs .thumb').forEach((t) => t.classList.toggle('active', t === thumb));
    });
  });

  // ---- Ô nhập tiền: tự thêm dấu chấm ngăn cách hàng nghìn khi gõ (1500000 → 1.500.000) ----
  document.querySelectorAll('input.js-money').forEach((input) => {
    const format = () => {
      const digits = input.value.replace(/\D/g, '').replace(/^0+(?=\d)/, '');
      input.value = digits ? Number(digits).toLocaleString('vi-VN') : '';
    };
    input.addEventListener('input', format);
    format();
  });

  // ---- Form đăng bài ----
  const form = document.querySelector('form.product-form');
  if (!form) return;

  // Hiện ô giá bán hay ô trao đổi theo loại bài đang chọn
  const typeInputs = form.querySelectorAll('input[name="listingType"]');
  function syncListingType() {
    const type = form.querySelector('input[name="listingType"]:checked')?.value || 'sale';
    form.querySelectorAll('[data-show-for]').forEach((box) => {
      const show = box.dataset.showFor.split(' ').includes(type);
      box.hidden = !show;
      box.querySelectorAll('input.js-money').forEach((i) => { i.required = show; });
    });
  }
  typeInputs.forEach((i) => i.addEventListener('change', syncListingType));
  syncListingType();

  // Danh mục con chỉ hiện những mục thuộc danh mục cha đang chọn
  const parent = document.getElementById('parentCategory');
  const child = document.getElementById('childCategory');
  const childField = document.getElementById('childCategoryField');
  function syncChildren() {
    let any = false;
    child.querySelectorAll('option[data-parent]').forEach((o) => {
      const match = o.dataset.parent === parent.value;
      o.hidden = !match;
      o.disabled = !match;
      if (match) any = true;
      if (!match && o.selected) child.value = '';
    });
    childField.hidden = !any;
  }
  parent.addEventListener('change', syncChildren);
  syncChildren();

  // Đếm số ký tự mô tả
  form.querySelectorAll('textarea[data-counter]').forEach((area) => {
    const counter = document.getElementById(area.dataset.counter);
    const update = () => { counter.textContent = area.value.length + ' / ' + area.maxLength; };
    area.addEventListener('input', update);
    update();
  });

  // Ảnh: xem trước ảnh mới chọn, bỏ chọn ảnh cũ, giới hạn số ảnh
  const max = Number(form.dataset.maxImages) || 8;
  const grid = document.getElementById('photoGrid');
  const input = document.getElementById('photoInput');
  const addTile = document.getElementById('photoAdd');
  let picked = new DataTransfer();   // danh sách file mới, cho phép chọn nhiều lần và bỏ từng ảnh

  const keptCount = () => grid.querySelectorAll('.photo-tile.existing input:checked').length;

  function render() {
    grid.querySelectorAll('.photo-tile.new').forEach((t) => t.remove());
    Array.from(picked.files).forEach((file, index) => {
      const tile = document.createElement('div');
      tile.className = 'photo-tile new';
      const img = document.createElement('img');
      img.src = URL.createObjectURL(file);
      img.alt = file.name;
      const remove = document.createElement('button');
      remove.type = 'button';
      remove.className = 'photo-remove';
      remove.title = 'Bỏ ảnh này';
      remove.textContent = '✕';
      remove.addEventListener('click', () => {
        const next = new DataTransfer();
        Array.from(picked.files).forEach((f, i) => { if (i !== index) next.items.add(f); });
        picked = next;
        input.files = picked.files;
        render();
      });
      tile.append(img, remove);
      grid.insertBefore(tile, addTile);
    });
    const total = keptCount() + picked.files.length;
    addTile.hidden = total >= max;
    input.required = total === 0;
  }

  input.addEventListener('change', () => {
    const tooBig = [];
    Array.from(input.files).forEach((file) => {
      if (file.size > 5 * 1024 * 1024) { tooBig.push(file.name); return; }
      if (keptCount() + picked.files.length < max) picked.items.add(file);
    });
    if (tooBig.length) toast('Ảnh quá 5 MB: ' + tooBig.join(', '));
    input.files = picked.files;
    render();
  });
  grid.addEventListener('change', (e) => { if (e.target.name === 'keepImage') render(); });
  render();
})();
