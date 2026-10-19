/*
 * Ô mật khẩu dễ dùng (đăng ký, đổi mật khẩu, đăng nhập):
 *  - Mỗi ô mật khẩu có nút "Hiện"/"Ẩn" để xem lại chữ đã gõ (người lớn tuổi dễ gõ sai).
 *  - Ô có data-pw-new: hiện danh sách điều kiện, tick ✓ từng điều kiện khi gõ, chưa đủ thì không cho gửi form.
 *  - Ô có data-pw-confirm="id ô mật khẩu mới": báo khớp / chưa khớp, chưa khớp thì không cho gửi.
 * Luật giống hệt PasswordUtil.checkStrength ở server: 8–128 ký tự, có chữ cái, có chữ số, không khoảng trắng.
 */
(function () {
    'use strict';

    var RULES = [
        { text: 'Ít nhất 8 ký tự', test: function (v) { return v.length >= 8 && v.length <= 128; } },
        { text: 'Có chữ cái', test: function (v) { return /\p{L}/u.test(v); } },
        { text: 'Có chữ số', test: function (v) { return /\d/.test(v); } },
        { text: 'Không có khoảng trắng', test: function (v) { return v.length > 0 && !/\s/.test(v); } }
    ];

    // Bọc ô mật khẩu và thêm nút Hiện/Ẩn bên phải
    function addToggle(input) {
        var wrap = document.createElement('span');
        wrap.className = 'pw-field';
        input.parentNode.insertBefore(wrap, input);
        wrap.appendChild(input);

        var btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'pw-toggle';
        btn.textContent = 'Hiện';
        btn.setAttribute('aria-label', 'Hiện mật khẩu');
        btn.addEventListener('click', function () {
            var show = input.type === 'password';
            input.type = show ? 'text' : 'password';
            btn.textContent = show ? 'Ẩn' : 'Hiện';
            btn.setAttribute('aria-label', show ? 'Ẩn mật khẩu' : 'Hiện mật khẩu');
            input.focus();
        });
        wrap.appendChild(btn);

        // Nút cao bằng ô nhập (ô có thể có margin-bottom riêng của từng trang)
        function fit() { btn.style.height = input.offsetHeight + 'px'; }
        fit();
        window.addEventListener('resize', fit);
    }

    // Danh sách điều kiện dưới ô mật khẩu mới
    function addRules(input) {
        var list = document.createElement('ul');
        list.className = 'pw-rules';
        list.setAttribute('aria-live', 'polite');
        var items = RULES.map(function (rule) {
            var li = document.createElement('li');
            li.textContent = rule.text;
            list.appendChild(li);
            return li;
        });
        input.closest('.pw-field').insertAdjacentElement('afterend', list);

        // Trang đăng ký có sẵn dòng gợi ý chữ, thay bằng danh sách này
        var hint = list.nextElementSibling;
        if (hint && hint.classList.contains('hint')) hint.hidden = true;

        function check() {
            var v = input.value, firstError = '';
            RULES.forEach(function (rule, i) {
                var ok = rule.test(v);
                items[i].classList.toggle('ok', ok);
                if (!ok && !firstError) firstError = 'Mật khẩu chưa đạt: ' + rule.text.toLowerCase() + '.';
            });
            input.setCustomValidity(firstError);
        }
        input.addEventListener('input', check);
        check();
    }

    // Dòng báo "khớp / chưa khớp" dưới ô nhập lại
    function addConfirm(input, target) {
        var note = document.createElement('p');
        note.className = 'pw-match';
        note.setAttribute('aria-live', 'polite');
        input.closest('.pw-field').insertAdjacentElement('afterend', note);

        function check() {
            if (!input.value) {
                note.textContent = '';
                note.className = 'pw-match';
                input.setCustomValidity('');
                return;
            }
            var same = input.value === target.value;
            note.textContent = same ? '✓ Hai mật khẩu đã khớp' : '✗ Hai mật khẩu chưa khớp';
            note.className = 'pw-match ' + (same ? 'ok' : 'bad');
            input.setCustomValidity(same ? '' : 'Mật khẩu nhập lại chưa khớp.');
        }
        input.addEventListener('input', check);
        target.addEventListener('input', check);
        // Gõ chưa xong mà đã ra khỏi ô thì vẫn báo
        input.addEventListener('blur', check);
    }

    document.querySelectorAll('input[type="password"]').forEach(addToggle);

    document.querySelectorAll('input[data-pw-new]').forEach(addRules);
    document.querySelectorAll('input[data-pw-confirm]').forEach(function (input) {
        var target = document.getElementById(input.getAttribute('data-pw-confirm'));
        if (target) addConfirm(input, target);
    });
})();
