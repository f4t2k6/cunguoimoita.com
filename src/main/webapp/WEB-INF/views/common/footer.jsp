<%@ page pageEncoding="UTF-8" %>
</main>
<%-- Khung chat nổi góc phải dưới kiểu Messenger (chat-dock.js). Trang Tin nhắn đặt hideChatDock để ẩn. --%>
<c:if test="${sessionScope.userRole == 'customer' and empty hideChatDock}">
  <div class="chat-dock" id="chatDock" data-ctx="${ctx}">
    <div class="dock-windows"></div>
    <div class="dock-panel" hidden>
      <div class="dock-panel-head">
        <strong>Đoạn chat</strong>
        <a href="${ctx}/chat/inbox">Xem tất cả</a>
      </div>
      <ul class="dock-list"><li class="dock-empty">Đang tải...</li></ul>
    </div>
    <div class="dock-side">
      <div class="dock-heads"></div>
      <button type="button" class="dock-launcher" aria-label="Đoạn chat" aria-haspopup="true" aria-expanded="false" title="Đoạn chat">
        <img src="${ctx}/assets/images/messenger-icon.png" alt="" aria-hidden="true">
        <span class="count-badge" hidden></span>
      </button>
    </div>
  </div>
  <script src="${ctx}/assets/js/chat.js?v=${assetVer}"></script>
  <script src="${ctx}/assets/js/chat-dock.js?v=${assetVer}"></script>
</c:if>
<script src="${pageContext.request.contextPath}/assets/js/common.js?v=${assetVer}"></script>
</body>
</html>
