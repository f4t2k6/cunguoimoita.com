// F7 khung chat kiểu Messenger (TV5). Tải tin nhắn qua /chat/messages, hỏi tin mới mỗi 3 giây.
// TODO: chuyển sang Jakarta WebSocket để tin đến ngay, không cần hỏi định kỳ.
(function () {
  const box = document.getElementById('chat-box');
  if (!box) return;
  const ctx = box.dataset.ctx;
  const conversationId = box.dataset.conversation;
  const otherInitial = box.dataset.otherInitial || '?';
  const errorEl = document.getElementById('chat-error');
  const GAP_MS = 15 * 60 * 1000;   // cách nhau hơn 15 phút thì hiện mốc giờ ở giữa
  let messages = [];               // toàn bộ tin đã tải, để vẽ lại theo nhóm
  let lastSentAt = 0;

  const money = n => Number(n).toLocaleString('vi-VN') + ' đ';
  const STATUS = { pending: 'Đang chờ trả lời', accepted: 'Đã chấp nhận', declined: 'Đã từ chối', expired: 'Hết hạn' };

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

  function el(tag, cls, text) {
    const e = document.createElement(tag);
    if (cls) e.className = cls;
    if (text != null) e.textContent = text;
    return e;
  }

  function offerCard(o) {
    const card = el('div', 'msgr-offer');
    card.append(el('span', 'msgr-offer-label', 'Trả giá'), el('strong', null, money(o.amount)));
    const status = el('span', 'msgr-offer-status offer-' + o.status, STATUS[o.status]);
    if (o.status === 'pending') {
      status.textContent += ' · hết hạn ' + new Date(o.expiresAt).toLocaleString('vi-VN',
          { hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit' });
    }
    card.append(status);
    if (o.canRespond) {
      const actions = el('div', 'msgr-offer-actions');
      const btn = (label, cls, fn) => {
        const b = el('button', 'msgr-pill ' + cls, label);
        b.type = 'button';
        b.onclick = () => fn().catch(e => showError(e.message));
        actions.append(b);
      };
      btn('Chấp nhận', '', () => post('/chat/offer', { action: 'accept', offerId: o.id }));
      btn('Từ chối', 'light', () => post('/chat/offer', { action: 'decline', offerId: o.id }));
      btn('Trả giá lại', 'light', () => {
        const v = prompt('Giá bạn muốn trả lại (đ):');
        return v ? post('/chat/offer', { action: 'counter', offerId: o.id, amount: v }) : Promise.resolve();
      });
      card.append(actions);
    }
    return card;
  }

  function timeLabel(ms) {
    const d = new Date(ms);
    const today = new Date().toDateString() === d.toDateString();
    return today
        ? d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
        : d.toLocaleString('vi-VN', { hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit' });
  }

  // Vẽ lại cả khung: tin liên tiếp của cùng một người gộp thành nhóm, avatar người kia ở tin cuối nhóm
  function renderAll() {
    const atBottom = box.scrollHeight - box.scrollTop - box.clientHeight < 60;
    box.innerHTML = '';
    if (!messages.length) {
      box.append(el('p', 'msgr-hint', 'Hãy gửi lời chào hoặc trả giá cho món này.'));
    }
    messages.forEach((m, i) => {
      const prev = messages[i - 1];
      const next = messages[i + 1];
      if (!prev || m.sentAt - prev.sentAt > GAP_MS) {
        box.append(el('div', 'msgr-time', timeLabel(m.sentAt)));
      }
      const sameAsPrev = prev && prev.mine === m.mine && m.sentAt - prev.sentAt <= GAP_MS;
      const sameAsNext = next && next.mine === m.mine && next.sentAt - m.sentAt <= GAP_MS;
      const row = el('div', 'msgr-row ' + (m.mine ? 'mine' : 'theirs')
          + (sameAsPrev ? ' join-prev' : '') + (sameAsNext ? ' join-next' : ''));
      if (!m.mine) {
        row.append(el('span', 'msgr-avatar small' + (sameAsNext ? ' ghost' : ''), otherInitial));
      }
      const content = m.offer ? offerCard(m.offer) : el('div', 'msgr-bubble', m.content);
      content.title = new Date(m.sentAt).toLocaleString('vi-VN');
      row.append(content);
      box.append(row);
    });
    if (atBottom) box.scrollTop = box.scrollHeight;
  }

  // full = true: tải lại toàn bộ (sau khi trả lời offer, để khung giá cũ cập nhật trạng thái)
  async function load(full) {
    const after = full ? '' : lastSentAt || '';
    const res = await fetch(ctx + '/chat/messages?id=' + encodeURIComponent(conversationId) + '&after=' + after);
    const json = await res.json();
    if (!json.ok) { showError(json.error); return; }
    if (!full && !json.messages.length) return;
    // Tin mới có thể làm khung giá cũ đổi trạng thái (người kia trả lời) → tải lại toàn bộ cho chắc
    if (!full) { await load(true); return; }
    messages = json.messages;
    messages.forEach(m => { lastSentAt = Math.max(lastSentAt, m.sentAt); });
    renderAll();
  }

  const chatForm = document.getElementById('chat-form');
  if (chatForm) {
    chatForm.addEventListener('submit', e => {
      e.preventDefault();
      const input = chatForm.content;
      if (!input.value.trim()) return;
      const text = input.value;
      input.value = '';
      box.scrollTop = box.scrollHeight;
      post('/chat/send', { conversationId, content: text })
        .then(() => { box.scrollTop = box.scrollHeight; })
        .catch(err => { input.value = text; showError(err.message); });
    });
    const offerForm = document.getElementById('offer-form');
    const toggleOffer = show => {
      offerForm.hidden = !show;
      if (show) offerForm.amount.focus();
    };
    document.getElementById('offer-toggle').onclick = () => toggleOffer(offerForm.hidden);
    document.getElementById('offer-cancel').onclick = () => toggleOffer(false);
    offerForm.addEventListener('submit', e => {
      e.preventDefault();
      post('/chat/offer', { action: 'make', conversationId, amount: offerForm.amount.value })
        .then(() => { offerForm.amount.value = ''; toggleOffer(false); box.scrollTop = box.scrollHeight; })
        .catch(err => showError(err.message));
    });
  }

  load(true).then(() => { box.scrollTop = box.scrollHeight; });
  setInterval(() => load(false).catch(() => {}), 3000);
})();
