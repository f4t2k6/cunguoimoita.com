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
        <svg viewBox="0 0 24 24" aria-hidden="true"><path fill="#fff" d="M12 2.5C6.5 2.5 2.3 6.5 2.3 11.6c0 2.7 1.1 5 3 6.6v3.4l3.2-1.8c1.1.3 2.2.5 3.5.5 5.5 0 9.7-4 9.7-9.1S17.5 2.5 12 2.5zm1 12.2l-2.5-2.6-4.8 2.6 5.3-5.6 2.5 2.6 4.7-2.6-5.2 5.6z"/></svg>
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
