// F7 khung chat (TV5). Tải tin nhắn qua /chat/messages, hỏi tin mới mỗi 3 giây.
// TODO: chuyển sang Jakarta WebSocket để tin đến ngay, không cần hỏi định kỳ.
(function () {
  const box = document.getElementById('chat-box');
  if (!box) return;
  const ctx = box.dataset.ctx;
  const conversationId = box.dataset.conversation;
  const errorEl = document.getElementById('chat-error');
  let lastSentAt = 0;
  const offerCards = {};           // offerId -> phần tử khung giá, để cập nhật trạng thái

  const money = n => Number(n).toLocaleString('vi-VN') + ' đ';
  const STATUS = { pending: 'Đang chờ', accepted: 'Đã chấp nhận', declined: 'Đã từ chối', expired: 'Hết hạn' };

  function showError(msg) {
    errorEl.textContent = msg || '';
    errorEl.hidden = !msg;
  }

  async function post(url, data) {
    const res = await fetch(ctx + url, { method: 'POST', body: new URLSearchParams(data) });
    const json = await res.json();
    if (!json.ok) throw new Error(json.error);
    showError('');
    await load(true);
  }

  function renderOffer(o, el) {
    el.innerHTML = '';
    const amount = document.createElement('strong');
    amount.textContent = 'Trả giá: ' + money(o.amount);
    const status = document.createElement('span');
    status.className = 'offer-status';
    status.textContent = STATUS[o.status] +
        (o.status === 'pending' ? ' · hết hạn ' + new Date(o.expiresAt).toLocaleString('vi-VN') : '');
    el.append(amount, status);
    if (o.canRespond) {
      const actions = document.createElement('div');
      actions.className = 'offer-actions';
      const btn = (label, cls, fn) => {
        const b = document.createElement('button');
        b.type = 'button'; b.textContent = label; if (cls) b.className = cls;
        b.onclick = () => fn().catch(e => showError(e.message));
        actions.append(b);
      };
      btn('Chấp nhận', '', () => post('/chat/offer', { action: 'accept', offerId: o.id }));
      btn('Từ chối', 'secondary', () => post('/chat/offer', { action: 'decline', offerId: o.id }));
      btn('Trả giá lại', 'secondary', () => {
        const v = prompt('Giá bạn muốn trả lại (đ):');
        return v ? post('/chat/offer', { action: 'counter', offerId: o.id, amount: v }) : Promise.resolve();
      });
      el.append(actions);
    }
  }

  function render(m) {
    const row = document.createElement('div');
    row.className = 'msg ' + (m.mine ? 'mine' : 'theirs');
    const bubble = document.createElement('div');
    bubble.className = 'bubble' + (m.offer ? ' offer' : '');
    if (m.offer) {
      offerCards[m.offer.id] = bubble;
      renderOffer(m.offer, bubble);
    } else {
      bubble.textContent = m.content;
    }
    const time = document.createElement('time');
    time.textContent = new Date(m.sentAt).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
    row.append(bubble, time);
    box.append(row);
  }

  // full = true: tải lại toàn bộ (sau khi trả lời offer, để khung giá cũ cập nhật trạng thái)
  async function load(full) {
    const after = full ? '' : lastSentAt || '';
    const res = await fetch(ctx + '/chat/messages?id=' + encodeURIComponent(conversationId) + '&after=' + after);
    const json = await res.json();
    if (!json.ok) { showError(json.error); return; }
    if (full) { box.innerHTML = ''; }
    json.messages.forEach(m => {
      render(m);
      lastSentAt = Math.max(lastSentAt, m.sentAt);
    });
    // Tin mới có thể làm khung giá cũ đổi trạng thái (người kia trả lời) → tải lại toàn bộ cho chắc
    if (!full && json.messages.length) { await load(true); return; }
    if (json.messages.length) box.scrollTop = box.scrollHeight;
  }

  const chatForm = document.getElementById('chat-form');
  if (chatForm) {
    chatForm.addEventListener('submit', e => {
      e.preventDefault();
      const input = chatForm.content;
      if (!input.value.trim()) return;
      post('/chat/send', { conversationId, content: input.value })
        .then(() => { input.value = ''; })
        .catch(err => showError(err.message));
    });
    const offerForm = document.getElementById('offer-form');
    document.getElementById('offer-toggle').onclick = () => { offerForm.hidden = !offerForm.hidden; };
    offerForm.addEventListener('submit', e => {
      e.preventDefault();
      post('/chat/offer', { action: 'make', conversationId, amount: offerForm.amount.value })
        .then(() => { offerForm.amount.value = ''; offerForm.hidden = true; })
        .catch(err => showError(err.message));
    });
  }

  load(true);
  setInterval(() => load(false).catch(() => {}), 3000);
})();
