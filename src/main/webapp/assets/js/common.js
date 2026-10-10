// Dùng chung cho mọi trang (nạp ở footer.jsp).
(function () {
  const ctx = document.body.dataset.ctx || '';

  // Số trên chuông thông báo và mục Tin nhắn (F7), cập nhật mỗi 20 giây
  function setBadge(id, count) {
    const el = document.getElementById(id);
    if (!el) return;
    el.textContent = count > 9 ? '9+' : String(count);
    el.hidden = !(count > 0);
  }
  async function refreshBadges() {
    try {
      const [n, m] = await Promise.all([
        fetch(ctx + '/notifications/count').then(r => r.ok ? r.json() : null),
        fetch(ctx + '/chat/unread').then(r => r.ok ? r.json() : null)
      ]);
      if (n && n.ok) setBadge('notifyBadge', n.count);
      if (m && m.ok) setBadge('chatBadge', m.count);
    } catch (e) { /* chưa đăng nhập hoặc mất mạng: bỏ qua */ }
  }
  refreshBadges();
  setInterval(refreshBadges, 20000);

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
    let size = 22;
    name.style.fontSize = size + 'px';
    while (name.scrollWidth > name.parentElement.clientWidth && size > 10) {
      name.style.fontSize = (--size) + 'px';
    }
  }
})();
