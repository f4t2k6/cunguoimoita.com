// Khung chat nổi góc phải dưới, kiểu Messenger của Facebook (footer.jsp, mọi trang trừ trang Tin nhắn).
//  - Nút tin nhắn tròn: bấm mở danh sách đoạn chat (/chat/list), số tin chưa đọc lấy từ common.js (sự kiện chat:count).
//  - Phía trên nút: avatar 3 người nhắn gần nhất, bấm để mở cửa sổ chat nhỏ.
//  - Cửa sổ chat dùng lại ChatThread trong chat.js nên vẫn trả giá, chấp nhận / từ chối / trả giá lại, gửi ảnh như trang gốc.
//  - Tối đa 2 cửa sổ (điện thoại 1); các cửa sổ đang mở được nhớ trong sessionStorage khi chuyển trang.
(function () {
  const dock = document.getElementById('chatDock');
  if (!dock || !window.ChatThread) return;
  const ctx = dock.dataset.ctx || '';
  const winBox = dock.querySelector('.dock-windows');
  const heads = dock.querySelector('.dock-heads');
  const launcher = dock.querySelector('.dock-launcher');
  const panel = dock.querySelector('.dock-panel');
  const list = panel.querySelector('.dock-list');
  const STORE = 'chatDock';
  const HEADS = 3;

  let conversations = [];
  const byId = new Map();
  const windows = new Map();      // id -> { el, thread, minimized }
  let lastCount = -1;

  const maxWindows = () => (window.innerWidth < 684 ? 1 : 2);
  const money = n => Number(n).toLocaleString('vi-VN') + ' đ';

  function el(tag, cls, text) {
    const e = document.createElement(tag);
    if (cls) e.className = cls;
    if (text != null) e.textContent = text;
    return e;
  }

  function avatar(c, cls) {
    const a = el('span', 'msgr-avatar ' + (cls || ''), c.otherAvatar ? null : c.otherInitial);
    if (c.otherAvatar) {
      const img = el('img');
      img.src = ctx + c.otherAvatar;
      img.alt = '';
      a.append(img);
    }
    return a;
  }

  // ---- Nhớ cửa sổ đang mở khi chuyển trang ----
  function save() {
    try {
      sessionStorage.setItem(STORE, JSON.stringify([...windows].map(([id, w]) => ({ id, min: w.minimized }))));
    } catch (e) { /* trình duyệt chặn lưu trữ: bỏ qua */ }
  }
  function restore() {
    try { return JSON.parse(sessionStorage.getItem(STORE) || '[]'); } catch (e) { return []; }
  }

  // ---- Danh sách đoạn chat ----
  async function loadList() {
    try {
      const res = await fetch(ctx + '/chat/list', { headers: { Accept: 'application/json' } });
      const json = await res.json();
      if (!json.ok) return;
      conversations = json.conversations;
      byId.clear();
      conversations.forEach(c => byId.set(c.id, c));
      renderHeads();
      renderList();
    } catch (e) { /* mất mạng: lần sau thử lại */ }
  }

  function renderHeads() {
    heads.replaceChildren(...conversations.slice(0, HEADS).map(c => {
      const b = el('button', 'dock-head' + (c.unread ? ' unread' : ''));
      b.type = 'button';
      b.title = c.otherName + ' · ' + c.productName;
      b.setAttribute('aria-label', 'Mở đoạn chat với ' + c.otherName);
      b.append(avatar(c));
      if (c.unread) b.append(el('span', 'count-badge', c.unread > 9 ? '9+' : String(c.unread)));
      b.onclick = () => openWindow(c.id);
      return b;
    }));
  }

  function renderList() {
    if (!conversations.length) {
      list.replaceChildren(el('li', 'dock-empty', 'Chưa có đoạn chat nào. Bấm "Nhắn tin" trên một bài đăng để bắt đầu.'));
      return;
    }
    list.replaceChildren(...conversations.map(c => {
      const li = el('li');
      const b = el('button', 'msgr-item' + (c.unread ? ' unread' : ''));
      b.type = 'button';
      const body = el('span', 'msgr-item-body');
      body.append(el('span', 'msgr-item-name', c.otherName),
          el('span', 'msgr-item-product', c.productName + (c.open ? '' : ' · đã bán')));
      const last = el('span', 'msgr-item-last');
      last.append(el('span', 'msgr-item-text', c.preview || 'Bắt đầu trò chuyện'),
          el('span', 'msgr-item-time', '· ' + c.time));
      body.append(last);
      b.append(avatar(c), body);
      if (c.unread) b.append(el('span', 'msgr-dot'));
      b.onclick = () => { setPanel(false); openWindow(c.id); };
      li.append(b);
      return li;
    }));
  }

  function setPanel(open) {
    panel.hidden = !open;
    launcher.setAttribute('aria-expanded', String(open));
    if (open) loadList();
  }
  launcher.addEventListener('click', e => { e.stopPropagation(); setPanel(panel.hidden); });
  document.addEventListener('click', e => {
    if (!panel.hidden && !e.target.closest('.dock-panel') && !e.target.closest('.dock-launcher')) setPanel(false);
  });
  document.addEventListener('keydown', e => { if (e.key === 'Escape') setPanel(false); });

  // common.js báo số tin chưa đọc mỗi lần cập nhật huy hiệu; đổi số thì tải lại danh sách (avatar, chấm chưa đọc)
  document.addEventListener('chat:count', e => {
    const n = e.detail || 0;
    const badge = launcher.querySelector('.count-badge');
    badge.textContent = n > 9 ? '9+' : String(n);
    badge.hidden = !(n > 0);
    if (n !== lastCount) {
      lastCount = n;
      loadList();
    }
  });

  // ---- Cửa sổ chat nhỏ ----
  function productTemplate(c) {
    const tpl = document.createElement('template');
    tpl.setAttribute('data-chat-product', '');
    const card = el('div', 'msgr-product');
    const photo = el('a', 'msgr-product-photo');
    photo.href = ctx + '/product?id=' + encodeURIComponent(c.productId);
    if (c.productImage) {
      const img = el('img');
      img.src = ctx + c.productImage;
      img.alt = c.productName;
      photo.append(img);
    } else {
      photo.append(el('span', null, 'Chưa có ảnh'));
    }
    const info = el('div', 'msgr-product-info');
    info.append(el('span', 'msgr-product-label', c.asker ? 'Bạn đang hỏi mua' : 'Người mua đang hỏi về'));
    const name = el('a', 'msgr-product-name', c.productName);
    name.href = photo.href;
    info.append(name);
    if (c.productPrice != null) info.append(el('strong', 'msgr-product-price', 'Giá đăng: ' + money(c.productPrice)));
    if (c.productCondition) info.append(el('span', 'msgr-product-meta', 'Tình trạng: ' + c.productCondition));
    if (c.open && c.asker) {
      const btn = el('button', 'msgr-pill', 'Trả giá món này');
      btn.type = 'button';
      btn.setAttribute('data-open-offer', '');
      info.append(btn);
    }
    card.append(photo, info);
    tpl.content.append(card);
    return tpl;
  }

  const ICONS = {
    offer: '<svg viewBox="0 0 24 24" fill="none" stroke="#000" stroke-width="1.8" stroke-linejoin="round"><path d="M3 12V4h8l10 10-8 8L3 12z"/><circle cx="7.5" cy="8.5" r="1.5"/></svg>',
    image: '<svg viewBox="0 0 24 24" fill="none" stroke="#000" stroke-width="1.8" stroke-linejoin="round"><rect x="3" y="5" width="18" height="14" rx="2"/><circle cx="9" cy="10" r="1.8"/><path d="M21 16l-5-5-8 8"/></svg>',
    send: '<svg viewBox="0 0 24 24" fill="#000"><path d="M3 20.5l18-8.5L3 3.5l2.5 7.5L15 12l-9.5 1z"/></svg>',
    expand: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M14 4h6v6M20 4l-8 8M10 20H4v-6"/></svg>',
    min: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><path d="M6 12h12"/></svg>',
    close: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><path d="M6 6l12 12M18 6L6 18"/></svg>'
  };
  function iconButton(cls, label, svg) {
    const b = el('button', cls);
    b.type = 'button';
    b.title = label;
    b.setAttribute('aria-label', label);
    b.innerHTML = svg;   // svg cố định ở trên, không chứa dữ liệu người dùng
    return b;
  }

  function buildWindow(c) {
    const win = el('section', 'dock-window');
    win.setAttribute('aria-label', 'Đoạn chat với ' + c.otherName);
    const head = el('header', 'dock-win-head');
    const who = el('button', 'dock-win-who');
    who.type = 'button';
    who.title = 'Thu nhỏ / mở rộng';
    const text = el('span', 'dock-win-text');
    text.append(el('strong', null, c.otherName),
        el('span', null, (c.asker ? 'Người bán' : 'Người mua') + ' · ' + c.productName));
    who.append(avatar(c), text);
    const full = el('a', 'dock-win-btn');
    full.href = ctx + '/chat/conversation?id=' + encodeURIComponent(c.id);
    full.title = 'Mở trang tin nhắn';
    full.setAttribute('aria-label', 'Mở trang tin nhắn');
    full.innerHTML = ICONS.expand;
    const min = iconButton('dock-win-btn', 'Thu nhỏ', ICONS.min);
    const close = iconButton('dock-win-btn', 'Đóng', ICONS.close);
    head.append(who, full, min, close);

    const body = el('div', 'dock-win-body');
    const thread = el('div', 'msgr-thread');
    thread.dataset.ctx = ctx;
    thread.dataset.conversation = c.id;
    thread.dataset.otherInitial = c.otherInitial;
    if (c.otherAvatar) thread.dataset.otherAvatar = c.otherAvatar;
    const error = el('p', 'msgr-error');
    error.hidden = true;
    body.append(thread, productTemplate(c), error);

    if (c.open) {
      const offer = el('form', 'msgr-offer-bar');
      offer.hidden = true;
      const amount = el('input');
      Object.assign(amount, { name: 'amount', type: 'text', inputMode: 'numeric', autocomplete: 'off',
        placeholder: 'Số tiền, VD 1.500.000' });
      amount.setAttribute('aria-label', 'Số tiền muốn trả (đồng)');
      const sendOffer = el('button', 'msgr-pill', 'Gửi');
      sendOffer.type = 'submit';
      const cancel = el('button', 'msgr-pill light', 'Huỷ');
      cancel.type = 'button';
      cancel.setAttribute('data-offer-cancel', '');
      offer.append(amount, el('span', 'msgr-offer-unit', 'đ'), sendOffer, cancel);

      const composer = el('form', 'msgr-composer');
      const toggle = iconButton('msgr-icon-btn', 'Trả giá', ICONS.offer);
      toggle.setAttribute('data-offer-toggle', '');
      const pick = el('label', 'msgr-icon-btn');
      pick.title = 'Gửi ảnh';
      pick.setAttribute('aria-label', 'Gửi ảnh');
      pick.innerHTML = ICONS.image;
      const file = el('input');
      Object.assign(file, { type: 'file', accept: 'image/*', hidden: true });
      pick.append(file);
      const input = el('input');
      Object.assign(input, { name: 'content', autocomplete: 'off', placeholder: 'Aa' });
      input.setAttribute('aria-label', 'Tin nhắn');
      const send = iconButton('msgr-icon-btn', 'Gửi', ICONS.send);
      send.type = 'submit';
      composer.append(toggle, pick, input, send);
      body.append(offer, composer);
    } else {
      body.append(el('p', 'msgr-closed', 'Món này đã bán, đoạn chat đã đóng.'));
    }
    win.append(head, body);

    who.onclick = () => setMinimized(c.id, !windows.get(c.id).minimized);
    min.onclick = () => setMinimized(c.id, true);
    close.onclick = () => closeWindow(c.id);
    return win;
  }

  function setMinimized(id, value) {
    const w = windows.get(id);
    if (!w) return;
    w.minimized = value;
    w.el.classList.toggle('minimized', value);
    if (!value) {
      const box = w.el.querySelector('.msgr-thread');
      box.scrollTop = box.scrollHeight;
      const input = w.el.querySelector('.msgr-composer input[name=content]');
      if (input) input.focus({ preventScroll: true });
    }
    save();
  }

  function openWindow(id, minimized) {
    const c = byId.get(id);
    if (!c) return;
    if (windows.has(id)) {
      setMinimized(id, false);
      return;
    }
    // Quá số cửa sổ cho phép thì đóng cửa sổ mở lâu nhất
    while (windows.size >= maxWindows()) closeWindow(windows.keys().next().value);
    const win = buildWindow(c);
    winBox.prepend(win);              // cửa sổ mới nằm sát nút tin nhắn
    const w = { el: win, thread: window.ChatThread(win.querySelector('.dock-win-body')), minimized: false };
    windows.set(id, w);
    setMinimized(id, !!minimized);
  }

  function closeWindow(id) {
    const w = windows.get(id);
    if (!w) return;
    if (w.thread) w.thread.stop();
    w.el.remove();
    windows.delete(id);
    save();
    loadList();                       // vừa đọc tin trong cửa sổ → cập nhật chấm chưa đọc
  }

  loadList().then(() => restore().forEach(s => openWindow(s.id, s.min)));
})();
