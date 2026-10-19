// Dùng chung cho mọi trang (nạp ở footer.jsp).
(function () {
  const ctx = document.body.dataset.ctx || '';

  // Số trên chuông thông báo và mục Tin nhắn (F7): 1 lần gọi /notifications/count trả cả 2 con số.
  // Cập nhật mỗi 20 giây, ngừng khi tab bị ẩn, cập nhật ngay khi quay lại tab hoặc khi trang khác báo 'badges:refresh'.
  function setBadge(id, count) {
    const el = document.getElementById(id);
    if (!el) return;
    el.textContent = count > 9 ? '9+' : String(count);
    el.hidden = !(count > 0);
  }
  let badgeTimer = null;
  async function refreshBadges() {
    clearTimeout(badgeTimer);
    if (!document.getElementById('notifyBtn')) return;   // khách chưa đăng nhập: không có thông báo
    try {
      const res = await fetch(ctx + '/notifications/count', { headers: { Accept: 'application/json' } });
      const json = res.ok ? await res.json() : null;
      if (json && json.ok) {
        setBadge('notifyBadge', json.count);
        setBadge('chatBadge', json.chat);
        // Khung chat nổi (chat-dock.js) cập nhật huy hiệu và danh sách theo con số này
        document.dispatchEvent(new CustomEvent('chat:count', { detail: json.chat }));
      }
    } catch (e) { /* chưa đăng nhập hoặc mất mạng: bỏ qua */ }
    if (!document.hidden) badgeTimer = setTimeout(refreshBadges, 20000);
  }
  refreshBadges();
  document.addEventListener('visibilitychange', () => {
    if (document.hidden) clearTimeout(badgeTimer); else refreshBadges();
  });
  document.addEventListener('badges:refresh', refreshBadges);

  // ---- Thông báo (F7): bấm nút Thông báo → danh sách xổ xuống; mỗi dòng có nút "⋯" mở menu ----
  const ago = ms => {
    const s = Math.max(0, (Date.now() - ms) / 1000);
    if (s < 60) return 'Vừa xong';
    if (s < 3600) return Math.floor(s / 60) + ' phút trước';
    if (s < 86400) return Math.floor(s / 3600) + ' giờ trước';
    if (s < 7 * 86400) return Math.floor(s / 86400) + ' ngày trước';
    return new Date(ms).toLocaleDateString('vi-VN');
  };
  const fillTimes = root => root.querySelectorAll('time[data-ts]').forEach(t => {
    t.textContent = ago(Number(t.dataset.ts));
  });
  fillTimes(document);

  function notifyItem(n) {
    const li = document.createElement('li');
    li.className = 'notify-item' + (n.read ? '' : ' unread');
    li.dataset.id = n.id;
    const openUrl = ctx + '/notifications/open?id=' + encodeURIComponent(n.id);
    const main = document.createElement('a');
    main.className = 'notify-main';
    main.href = openUrl;
    const icon = document.createElement('span');
    icon.className = 'notify-icon type-' + n.type;
    const text = document.createElement('span');
    text.className = 'notify-text';
    const content = document.createElement('span');
    content.className = 'notify-content';
    content.textContent = n.content;
    const time = document.createElement('time');
    time.className = 'notify-time';
    time.textContent = ago(n.createdAt);
    text.append(content, time);
    const dot = document.createElement('span');
    dot.className = 'notify-dot';
    main.append(icon, text, dot);

    const more = document.createElement('button');
    more.type = 'button';
    more.className = 'notify-more';
    more.setAttribute('aria-label', 'Tuỳ chọn');
    more.textContent = '⋯';
    const menu = document.createElement('div');
    menu.className = 'notify-menu';
    menu.hidden = true;
    const open = document.createElement('a');
    open.href = openUrl;
    open.textContent = 'Mở thông báo';
    menu.append(open);
    if (!n.read) {
      const mark = document.createElement('button');
      mark.type = 'button';
      mark.className = 'notify-mark-read';
      mark.textContent = 'Đánh dấu đã đọc';
      menu.append(mark);
    }
    li.append(main, more, menu);
    return li;
  }

  const notifyBtn = document.getElementById('notifyBtn');
  const notifyPanel = document.getElementById('notifyPanel');
  const notifyList = document.getElementById('notifyList');

  async function loadNotifications() {
    notifyList.innerHTML = '<li class="notify-empty">Đang tải...</li>';
    try {
      const json = await fetch(ctx + '/notifications/list').then(r => r.json());
      notifyList.innerHTML = '';
      if (!json.ok) throw new Error();
      if (!json.items.length) notifyList.innerHTML = '<li class="notify-empty">Chưa có thông báo nào.</li>';
      json.items.forEach(n => notifyList.append(notifyItem(n)));
    } catch (e) {
      notifyList.innerHTML = '<li class="notify-empty">Đăng nhập để xem thông báo.</li>';
    }
  }

  function closeMenus(except) {
    document.querySelectorAll('.notify-menu').forEach(m => { if (m !== except) m.hidden = true; });
  }
  function setPanel(open) {
    if (!notifyPanel) return;
    notifyPanel.hidden = !open;
    notifyBtn.setAttribute('aria-expanded', String(open));
    if (open) loadNotifications(); else closeMenus();
  }

  async function postJson(url) {
    await fetch(ctx + url, { method: 'POST', headers: { Accept: 'application/json' } });
    refreshBadges();
  }

  if (notifyBtn) {
    notifyBtn.addEventListener('click', e => { e.stopPropagation(); setPanel(notifyPanel.hidden); });
    document.getElementById('notifyReadAll').addEventListener('click', async () => {
      await postJson('/notifications/read-all');
      loadNotifications();
    });
  }

  // Nút "⋯" và "Đánh dấu đã đọc" (dùng chung cho danh sách xổ xuống và trang /notifications)
  document.addEventListener('click', async e => {
    const more = e.target.closest('.notify-more');
    if (more) {
      const menu = more.nextElementSibling;
      closeMenus(menu);
      menu.hidden = !menu.hidden;
      e.stopPropagation();
      return;
    }
    const mark = e.target.closest('.notify-mark-read');
    if (mark) {
      const li = mark.closest('.notify-item');
      await postJson('/notifications/read?id=' + encodeURIComponent(li.dataset.id));
      li.classList.remove('unread');
      mark.remove();
      closeMenus();
      return;
    }
    if (!e.target.closest('.notify-menu')) closeMenus();
    if (notifyPanel && !notifyPanel.hidden && !e.target.closest('#notifyPanel')) setPanel(false);
  });
  document.addEventListener('keydown', e => { if (e.key === 'Escape') setPanel(false); });

  // ---- Cài đặt ở menu trái: bấm để mở/đóng, bấm ra ngoài hoặc Esc thì đóng ----
  const settingsBtn = document.getElementById('settingsBtn');
  const settingsPanel = document.getElementById('settingsPanel');
  if (settingsBtn) {
    const setSettings = open => {
      settingsPanel.hidden = !open;
      settingsBtn.setAttribute('aria-expanded', String(open));
    };
    settingsBtn.addEventListener('click', e => { e.stopPropagation(); setSettings(settingsPanel.hidden); });
    document.addEventListener('click', e => { if (!e.target.closest('.menu-settings')) setSettings(false); });
    document.addEventListener('keydown', e => { if (e.key === 'Escape') setSettings(false); });
  }

  // ---- Nút "← Quay lại" đầu trang ----
  // Đến từ một trang khác trong web thì quay về đúng trang đó (giữ vị trí cuộn, bộ lọc...).
  // Mở thẳng link, hoặc trang trước chính là trang này (vừa lưu form xong) thì đi theo href (trang cha của trang này).
  document.querySelectorAll('[data-back]').forEach(link => link.addEventListener('click', e => {
    let prev = null;
    try { prev = document.referrer ? new URL(document.referrer) : null; } catch (err) { /* referrer lạ: bỏ qua */ }
    if (prev && prev.origin === location.origin && prev.pathname !== location.pathname && history.length > 1) {
      e.preventDefault();
      history.back();
    }
  }));

  // Thanh trên cùng: thêm bóng mờ khi đã cuộn xuống
  const topbar = document.querySelector('.topbar');
  if (topbar) {
    const onScroll = () => topbar.classList.toggle('scrolled', window.scrollY > 0);
    window.addEventListener('scroll', onScroll, { passive: true });
    onScroll();
  }

  // Tên người dùng dài thì thu nhỏ chữ cho vừa khung
  const name = document.getElementById('usernameText');
  if (name) {
    let size = 20;
    name.style.fontSize = size + 'px';
    while (name.scrollWidth > name.parentElement.clientWidth && size > 9) {
      name.style.fontSize = (--size) + 'px';
    }
  }
})();
