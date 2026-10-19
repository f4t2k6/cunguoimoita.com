// F7 khung chat kiểu Messenger (TV5). Tải tin nhắn qua /chat/messages rồi định kỳ hỏi tin mới.
// Để chạy nhẹ: chỉ hỏi phần tin mới (after), không hỏi khi tab bị ẩn, giãn dần nhịp hỏi khi không có tin,
// không gửi 2 yêu cầu chồng nhau, và chỉ vẽ lại khi thật sự có thay đổi.
// TODO: chuyển sang Jakarta WebSocket để tin đến ngay, không cần hỏi định kỳ.
(function () {
  const box = document.getElementById('chat-box');
  if (!box) return;
  const ctx = box.dataset.ctx;
  const conversationId = box.dataset.conversation;
  const otherInitial = box.dataset.otherInitial || '?';
  const errorEl = document.getElementById('chat-error');
  const GAP_MS = 15 * 60 * 1000;   // cách nhau hơn 15 phút thì hiện mốc giờ ở giữa

  let messages = [];               // toàn bộ tin đã tải, cũ trước mới sau
  const seen = new Set();          // id các tin đã có, để không thêm trùng
  let pending = [];                // tin vừa gõ, đang gửi (hiện mờ cho tới khi server nhận)
  let lastSentAt = 0;
  let idlePolls = 0;               // số lần hỏi liên tiếp không có tin mới
  let timer = null;
  let busy = false;
  let again = false;               // có yêu cầu hỏi trong lúc đang hỏi → hỏi lại ngay khi xong
  let openOfferForm = () => {};    // mở/đóng thanh trả giá (gán lại bên dưới khi đoạn chat còn mở)
  let counter = null;              // ô trả giá lại đang mở: { offerId, amount, focus } (giữ lại khi khung chat vẽ lại)

  const money = n => Number(n).toLocaleString('vi-VN') + ' đ';
  const productTpl = document.getElementById('chat-product');

  // Ô nhập tiền: gõ tới đâu tự thêm dấu chấm phần nghìn tới đó (1500000 → 1.500.000), giữ đúng vị trí con trỏ.
  // Khi gửi lên server dùng moneyValue() để lấy lại số không có dấu chấm.
  function moneyInput(input) {
    input.addEventListener('input', () => {
      const caret = input.selectionStart;
      const digitsBefore = input.value.slice(0, caret).replace(/\D/g, '').length;
      const digits = input.value.replace(/\D/g, '').replace(/^0+(?=\d)/, '').slice(0, 10);
      input.value = digits ? Number(digits).toLocaleString('vi-VN') : '';
      // Đặt con trỏ ngay sau chữ số thứ digitsBefore
      let pos = 0, seenDigits = 0;
      while (pos < input.value.length && seenDigits < digitsBefore) {
        if (/\d/.test(input.value[pos])) seenDigits++;
        pos++;
      }
      input.setSelectionRange(pos, pos);
    });
  }
  const moneyValue = input => input.value.replace(/\D/g, '');
  function checkMoney(input) {
    const v = Number(moneyValue(input));
    if (!v) return 'Vui lòng nhập số tiền';
    if (v < 1000) return 'Số tiền tối thiểu là 1.000 đ';
    return '';
  }
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
    idlePolls = 0;
    await poll();
  }

  function el(tag, cls, text) {
    const e = document.createElement(tag);
    if (cls) e.className = cls;
    if (text != null) e.textContent = text;
    return e;
  }

  function offerCard(o) {
    // Offer quá hạn thì hiện "Hết hạn" ngay, không cần chờ server
    const status = o.status === 'pending' && Date.now() > o.expiresAt ? 'expired' : o.status;
    const card = el('div', 'msgr-offer');
    card.append(el('span', 'msgr-offer-label', 'Trả giá'), el('strong', null, money(o.amount)));
    const statusEl = el('span', 'msgr-offer-status offer-' + status, STATUS[status]);
    if (status === 'pending') {
      statusEl.textContent += ' · hết hạn ' + new Date(o.expiresAt).toLocaleString('vi-VN',
          { hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit' });
    }
    card.append(statusEl);
    if (o.canRespond && status === 'pending') {
      const actions = el('div', 'msgr-offer-actions');
      const btn = (label, cls, fn) => {
        const b = el('button', 'msgr-pill ' + cls, label);
        b.type = 'button';
        b.onclick = () => {
          actions.querySelectorAll('button').forEach(x => { x.disabled = true; });
          fn().catch(e => showError(e.message))
              .finally(() => actions.querySelectorAll('button').forEach(x => { x.disabled = false; }));
        };
        actions.append(b);
      };
      btn('Chấp nhận', '', () => post('/chat/offer', { action: 'accept', offerId: o.id }));
      btn('Từ chối', 'light', () => post('/chat/offer', { action: 'decline', offerId: o.id }));
      btn('Trả giá lại', 'light', () => {
        counter = { offerId: o.id, amount: '', focus: true };
        renderAll();
        return Promise.resolve();
      });
      card.append(counter && counter.offerId === o.id ? counterForm(o) : actions);
    }
    return card;
  }

  // Ô "Trả giá lại" ngay trong khung giá (thay cho hộp prompt của trình duyệt)
  function counterForm(o) {
    const form = el('form', 'msgr-counter');
    const input = el('input');
    Object.assign(input, { type: 'text', inputMode: 'numeric', autocomplete: 'off', name: 'amount', required: true,
      placeholder: 'Giá mới, VD 1.500.000', value: counter.amount });
    input.setAttribute('aria-label', 'Giá bạn muốn trả lại (đồng)');
    moneyInput(input);
    input.addEventListener('input', () => { counter.amount = input.value; });
    const send = el('button', 'msgr-pill', 'Gửi');
    send.type = 'submit';
    const cancel = el('button', 'msgr-pill light', 'Huỷ');
    cancel.type = 'button';
    cancel.onclick = () => { counter = null; renderAll(); };
    form.onsubmit = e => {
      e.preventDefault();
      const problem = checkMoney(input);
      if (problem) { showError(problem); input.focus(); return; }
      form.querySelectorAll('button, input').forEach(x => { x.disabled = true; });
      post('/chat/offer', { action: 'counter', offerId: o.id, amount: moneyValue(input) })
        .then(() => { counter = null; renderAll(); })
        .catch(err => {
          showError(err.message);
          form.querySelectorAll('button, input').forEach(x => { x.disabled = false; });
        });
    };
    form.append(input, send, cancel);
    return form;
  }

  // Nút nhỏ "Xem đơn" dẫn tới trang chi tiết đơn tạo từ giá đã chốt (orderId/orderCode server gửi kèm offer accepted)
  function orderLink(o) {
    const a = el('a', 'msgr-order-link', 'Xem đơn ' + o.orderCode + ' →');
    a.href = ctx + '/order/detail?id=' + encodeURIComponent(o.orderId);
    return a;
  }

  // Tin ảnh: content là đường dẫn /uploads/chat/<id>.jpg; bấm để mở ảnh lớn ở tab mới
  function imageMessage(m) {
    const a = el('a', 'msgr-image-link');
    a.href = ctx + m.content;
    a.target = '_blank';
    const img = el('img', 'msgr-image');
    img.src = ctx + m.content;
    img.alt = 'Ảnh';
    img.loading = 'lazy';
    a.append(img);
    return a;
  }

  // Tin "Đã chấp nhận giá ... Đơn ORD-xxx đã được tạo." → kèm nút xem đơn ngay dưới bong bóng
  function textMessage(m) {
    const bubble = el('div', 'msgr-bubble', m.content);
    const code = (m.content.match(/Đơn (ORD-[0-9A-F]+) đã được tạo/) || [])[1];
    const offer = code && messages.map(x => x.offer).find(x => x && x.orderCode === code);
    if (!offer) return bubble;
    const stack = el('div', 'msgr-stack');
    stack.append(bubble, orderLink(offer));
    return stack;
  }

  function timeLabel(ms) {
    const d = new Date(ms);
    const today = new Date().toDateString() === d.toDateString();
    return today
        ? d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
        : d.toLocaleString('vi-VN', { hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit' });
  }

  // Vẽ lại cả khung (dựng sẵn ngoài trang rồi gắn 1 lần): tin liên tiếp của cùng một người gộp thành nhóm,
  // avatar người kia ở tin cuối nhóm
  function renderAll() {
    const atBottom = box.scrollHeight - box.scrollTop - box.clientHeight < 60;
    const all = messages.concat(pending);
    const frag = document.createDocumentFragment();
    // Khung món đang trao đổi luôn nằm đầu đoạn chat
    if (productTpl) {
      const product = productTpl.content.cloneNode(true);
      const openOffer = product.querySelector('[data-open-offer]');
      if (openOffer) openOffer.onclick = () => openOfferForm(true);
      frag.append(product);
    }
    if (!all.length) {
      frag.append(el('p', 'msgr-hint', 'Hãy gửi lời chào hoặc trả giá cho món này.'));
    }
    all.forEach((m, i) => {
      const prev = all[i - 1];
      const next = all[i + 1];
      if (!prev || m.sentAt - prev.sentAt > GAP_MS) {
        frag.append(el('div', 'msgr-time', timeLabel(m.sentAt)));
      }
      const sameAsPrev = prev && prev.mine === m.mine && m.sentAt - prev.sentAt <= GAP_MS;
      const sameAsNext = next && next.mine === m.mine && next.sentAt - m.sentAt <= GAP_MS;
      const row = el('div', 'msgr-row ' + (m.mine ? 'mine' : 'theirs')
          + (sameAsPrev ? ' join-prev' : '') + (sameAsNext ? ' join-next' : '')
          + (m.sending ? ' sending' : ''));
      if (!m.mine) {
        row.append(el('span', 'msgr-avatar small' + (sameAsNext ? ' ghost' : ''), otherInitial));
      }
      const content = m.offer ? offerCard(m.offer) : m.type === 'image' ? imageMessage(m) : textMessage(m);
      content.title = m.sending ? 'Đang gửi...' : new Date(m.sentAt).toLocaleString('vi-VN');
      row.append(content);
      frag.append(row);
    });
    // Ô trả giá lại: đặt con trỏ vào khi vừa mở, và giữ con trỏ ở đó nếu đang gõ dở lúc khung chat vẽ lại
    const typing = document.activeElement && document.activeElement.closest('.msgr-counter');
    box.replaceChildren(frag);
    const counterInput = box.querySelector('.msgr-counter input');
    if (counterInput && (typing || counter.focus)) {
      counter.focus = false;
      counterInput.focus({ preventScroll: true });
    }
    if (atBottom) box.scrollTop = box.scrollHeight;
  }

  // Lần đầu (full = true) lấy hết; các lần sau chỉ lấy tin mới hơn lastSentAt.
  // Khi có tin mới, server gửi kèm mọi offer để cập nhật trạng thái các khung giá cũ.
  // @return true nếu có tin mới
  async function load(full) {
    const after = full ? '' : lastSentAt || '';
    const res = await fetch(ctx + '/chat/messages?id=' + encodeURIComponent(conversationId) + '&after=' + after);
    const json = await res.json();
    if (!json.ok) { showError(json.error); return false; }
    const fresh = json.messages.filter(m => !seen.has(m.id));
    if (!full && !fresh.length) return false;
    if (full) { messages = []; seen.clear(); }
    fresh.forEach(m => {
      seen.add(m.id);
      messages.push(m);
      lastSentAt = Math.max(lastSentAt, m.sentAt);
      // Tin mình vừa gửi đã có trên server → bỏ bản tạm đang hiện mờ
      const i = m.mine ? pending.findIndex(p => p.content === m.content) : -1;
      if (i >= 0) pending.splice(i, 1);
    });
    const offers = new Map(json.offers.map(o => [o.id, o]));
    messages.forEach(m => { if (m.offer && offers.has(m.offer.id)) m.offer = offers.get(m.offer.id); });
    renderAll();
    // Vừa đọc tin của người kia → cập nhật số trên mục Tin nhắn ngay (common.js nghe sự kiện này)
    if (fresh.some(m => !m.mine)) document.dispatchEvent(new Event('badges:refresh'));
    return true;
  }

  // Nhịp hỏi: 3 giây khi đang trò chuyện, không có tin thì giãn dần tới 10 giây
  const nextDelay = () => (idlePolls < 10 ? 3000 : idlePolls < 30 ? 6000 : 10000);

  function schedule() {
    clearTimeout(timer);
    if (!document.hidden) timer = setTimeout(poll, nextDelay());
  }

  async function poll() {
    if (busy) { again = true; return; }
    busy = true;
    clearTimeout(timer);
    try {
      idlePolls = (await load(false)) ? 0 : idlePolls + 1;
    } catch (e) {
      idlePolls++;              // mất mạng: thử lại sau, giãn nhịp
    } finally {
      busy = false;
      if (again) { again = false; poll(); } else schedule();
    }
  }

  // Tab bị ẩn thì ngừng hỏi; quay lại thì hỏi ngay
  document.addEventListener('visibilitychange', () => {
    if (document.hidden) clearTimeout(timer);
    else { idlePolls = 0; poll(); }
  });

  const chatForm = document.getElementById('chat-form');
  if (chatForm) {
    chatForm.addEventListener('submit', e => {
      e.preventDefault();
      const input = chatForm.content;
      const text = input.value;
      if (!text.trim()) return;
      input.value = '';
      // Hiện tin ngay (mờ) trong lúc gửi, không chờ server
      const temp = { id: 'tmp-' + Date.now(), mine: true, content: text.trim(), sentAt: Date.now(), sending: true };
      pending.push(temp);
      renderAll();
      box.scrollTop = box.scrollHeight;
      const dropTemp = () => {
        if (!pending.includes(temp)) return;
        pending = pending.filter(p => p !== temp);
        renderAll();
      };
      post('/chat/send', { conversationId, content: text })
        .then(() => setTimeout(dropTemp, 10000))   // phòng khi không nhận lại được tin từ server
        .catch(err => { dropTemp(); input.value = text; showError(err.message); });
    });
    // Chọn ảnh xong là gửi luôn (multipart, 1 ảnh tối đa 5 MB)
    const imageInput = document.getElementById('chat-image');
    imageInput.addEventListener('change', async () => {
      const file = imageInput.files[0];
      imageInput.value = '';
      if (!file) return;
      if (file.size > 5 * 1024 * 1024) { showError('Ảnh tối đa 5 MB'); return; }
      const data = new FormData();
      data.append('conversationId', conversationId);
      data.append('image', file);
      try {
        const res = await fetch(ctx + '/chat/image', { method: 'POST', body: data });
        const json = await res.json().catch(() => ({ ok: false, error: 'Gửi ảnh không được' }));
        if (!json.ok) throw new Error(json.error);
        showError('');
        idlePolls = 0;
        await poll();
        box.scrollTop = box.scrollHeight;
      } catch (err) {
        showError(err.message);
      }
    });
    const offerForm = document.getElementById('offer-form');
    moneyInput(offerForm.amount);
    openOfferForm = show => {
      offerForm.hidden = !show;
      if (show) offerForm.amount.focus();
    };
    document.getElementById('offer-toggle').onclick = () => openOfferForm(offerForm.hidden);
    document.getElementById('offer-cancel').onclick = () => openOfferForm(false);
    offerForm.addEventListener('submit', e => {
      e.preventDefault();
      const problem = checkMoney(offerForm.amount);
      if (problem) { showError(problem); offerForm.amount.focus(); return; }
      post('/chat/offer', { action: 'make', conversationId, amount: moneyValue(offerForm.amount) })
        .then(() => { offerForm.amount.value = ''; openOfferForm(false); box.scrollTop = box.scrollHeight; })
        .catch(err => showError(err.message));
    });
  }

  load(true)
    .then(() => { box.scrollTop = box.scrollHeight; })
    .catch(() => showError('Không tải được tin nhắn, đang thử lại...'))
    .finally(schedule);
})();
