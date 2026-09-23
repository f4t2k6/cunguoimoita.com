<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>

<head>
  <meta charset="UTF-8">
  <title>Cunguoimoita</title>
  <link rel="stylesheet" href="css/style.css" type="text/css">
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Poppins:wght@300;400;500;600;700&display=swap" rel="stylesheet">
  <link href="https://fonts.googleapis.com/css2?family=Bungee&family=Playwrite+BE+WAL+Guides&display=swap" rel="stylesheet">
</head>

<body>


<img src="${pageContext.request.contextPath}/images/logo.png" alt="logo" class="logo">

<div class="search-bar">
  <img src="${pageContext.request.contextPath}/images/search-icon.png" alt="search" class="search-icon">
  <label>
    <input type="text" class="search-input" placeholder="Tìm kiếm trên cunguoimoita...">
  </label>
</div>

<div class="sidebar-menu">
  <button class="menu-item" type="button" onclick="location.href=''">
    <img src="${pageContext.request.contextPath}/images/cart-icon.png" alt="Giỏ hàng" class="menu-icon">
    <span>Giỏ hàng</span>
  </button>
  <button class="menu-item" type="button" onclick="location.href=''">
    <img src="${pageContext.request.contextPath}/images/message-icon.png" alt="Tin nhắn" class="menu-icon">
    <span>Tin nhắn</span>
  </button>
  <button class="menu-item" type="button" onclick="location.href=''">
    <img src="${pageContext.request.contextPath}/images/saved-icon.png" alt="Đã lưu" class="menu-icon">
    <span>Đã lưu</span>
  </button>
  <button class="menu-item" type="button" onclick="location.href=''">
    <img src="${pageContext.request.contextPath}/images/group-icon.png" alt="Hội nhóm" class="menu-icon">
    <span>Hội nhóm</span>
  </button>
</div>

<div class="profile-block">
  <img src="./images/avt.png" alt="avatar" class="profile-avatar">
  <div class="username-box">
    <span class="username" id="usernameText">USERNAME</span>
  </div>
</div>

<script>
  (function autoFitUsername() {
    const el = document.getElementById('usernameText');
    const box = el.parentElement; // .username-box
    let fontSize = 22; // font-size gốc (px), chỉnh theo ý bạn
    el.style.fontSize = fontSize + 'px';

    while (el.scrollWidth > box.clientWidth && fontSize > 10) {
      fontSize -= 1;
      el.style.fontSize = fontSize + 'px';
    }
  })();
</script>


</body>

</html>