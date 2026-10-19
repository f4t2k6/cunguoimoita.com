/*
 * Trang đăng nhập: "Quên mật khẩu" chạy ngay trong khung trắng, không chuyển trang.
 * Các bước gọi POST /forgot-password (ForgotPasswordServlet), nhận JSON {ok, error, message}.
 */
(function () {
    'use strict';

    var card = document.querySelector('.form-card');
    if (!card) return;
    var url = card.getAttribute('data-forgot-url');
    var views = card.querySelectorAll('.card-view');
    var resetEmail = '';
    var resendTimer = null;

    function view(name) {
        return card.querySelector('[data-view="' + name + '"]');
    }

    function show(name) {
        views.forEach(function (v) { v.hidden = v.getAttribute('data-view') !== name; });
        // password.js đo chiều cao ô lúc tải trang; ô đang ẩn thì đo lại khi hiện
        window.dispatchEvent(new Event('resize'));
        var first = view(name).querySelector('input:not([type=hidden])');
        if (first) first.focus();
    }

    function message(v, selector, text) {
        var el = v.querySelector(selector);
        if (!el) return;
        el.textContent = text || '';
        el.hidden = !text;
    }

    async function call(data) {
        var res = await fetch(url, {
            method: 'POST',
            headers: { 'X-Requested-With': 'fetch', Accept: 'application/json' },
            body: new URLSearchParams(data)
        });
        var json;
        try { json = await res.json(); } catch (e) { json = {}; }
        if (!res.ok || !json.ok) throw new Error(json.error || 'Có lỗi xảy ra, vui lòng thử lại.');
        return json;
    }

    // Đếm ngược nút "Gửi lại mã"
    function startResendCountdown(seconds) {
        var btn = card.querySelector('[data-resend]');
        clearInterval(resendTimer);
        var left = seconds || 60;
        btn.disabled = true;
        btn.textContent = 'Gửi lại mã (' + left + 's)';
        resendTimer = setInterval(function () {
            left--;
            if (left <= 0) {
                clearInterval(resendTimer);
                btn.disabled = false;
                btn.textContent = 'Gửi lại mã';
            } else {
                btn.textContent = 'Gửi lại mã (' + left + 's)';
            }
        }, 1000);
    }

    card.addEventListener('click', function (e) {
        var go = e.target.closest('[data-go]');
        if (!go) return;
        var target = go.getAttribute('data-go');
        if (target === 'forgot-email') {
            var typed = document.getElementById('email').value;
            var input = document.getElementById('forgotEmail');
            if (!input.value && typed) input.value = typed;
        }
        views.forEach(function (v) { message(v, '[data-error]', ''); });
        show(target);
    });

    card.querySelector('[data-resend]').addEventListener('click', async function () {
        var v = view('forgot-code');
        message(v, '[data-error]', '');
        try {
            var json = await call({ action: 'resend' });
            message(v, '[data-note]', json.message);
            startResendCountdown(json.resendSeconds);
        } catch (err) {
            message(v, '[data-note]', '');
            message(v, '[data-error]', err.message);
        }
    });

    card.querySelectorAll('form[data-step]').forEach(function (form) {
        form.addEventListener('submit', async function (e) {
            e.preventDefault();
            var v = form.closest('.card-view');
            var button = form.querySelector('button[type=submit]');
            var data = { action: form.getAttribute('data-step') };
            new FormData(form).forEach(function (value, key) { data[key] = value; });
            message(v, '[data-error]', '');
            button.disabled = true;
            try {
                var json = await call(data);
                if (data.action === 'send') {
                    resetEmail = data.email.trim();
                    view('forgot-code').querySelector('[data-reset-email]').textContent = resetEmail;
                    message(view('forgot-code'), '[data-note]', json.message);
                    document.getElementById('resetCode').value = '';
                    show('forgot-code');
                    startResendCountdown(json.resendSeconds);
                } else if (data.action === 'verify') {
                    show('forgot-reset');
                } else {
                    form.reset();
                    var login = view('login');
                    login.querySelectorAll('.form-error').forEach(function (el) { el.hidden = true; });
                    message(login, '[data-login-note]', json.message);
                    document.getElementById('email').value = json.email || resetEmail;
                    show('login');
                    document.getElementById('password').focus();
                }
            } catch (err) {
                message(v, '[data-error]', err.message);
            } finally {
                button.disabled = false;
            }
        });
    });
})();
