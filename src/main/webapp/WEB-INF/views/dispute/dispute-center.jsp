<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>

<jsp:include page="/WEB-INF/views/common/demo-header.jsp" />

<c:set var="disputeState" value="${not empty param.state ? param.state : 'reviewing'}" />

<div class="app-layout">
    <jsp:include page="/WEB-INF/views/common/sidebar.jsp" />

    <main class="main-content">
        <!-- THANH CÔNG CỤ DEMO KỊCH BẢN KHIẾU NẠI & HOÀN TIỀN (DÀNH CHO TUẤN ANH - F9) -->
        <section class="demo-toolbar">
            <div>
                <div class="demo-toolbar-title">🎛️ BẢNG ĐIỀU KHIỂN DEMO F9: KHIẾU NẠI (DISPUTE) &amp; HOÀN TIỀN (REFUND)</div>
                <div style="font-size: 12px; color: #d4d4d8;">
                    Chuyển nhanh trạng thái <code>DisputeStatus</code> để trình diễn quy trình xử lý tranh chấp từ lúc mở tới lúc hoàn tiền:
                </div>
            </div>
            <div style="display: flex; gap: 8px; flex-wrap: wrap;">
                <a href="${pageContext.request.contextPath}/ui?page=dispute&amp;state=open"
                   class="btn btn-sm ${disputeState == 'open' ? 'btn-amber' : 'btn-outline'}">
                    1. Mới gửi (open)
                </a>
                <a href="${pageContext.request.contextPath}/ui?page=dispute&amp;state=reviewing"
                   class="btn btn-sm ${disputeState == 'reviewing' ? 'btn-amber' : 'btn-outline'}">
                    2. Đang thẩm định (reviewing)
                </a>
                <a href="${pageContext.request.contextPath}/ui?page=dispute&amp;state=resolved_refund"
                   class="btn btn-sm ${disputeState == 'resolved_refund' ? 'btn-primary' : 'btn-outline'}">
                    3A. Buyer thắng → Hoàn tiền (resolved_refund)
                </a>
                <a href="${pageContext.request.contextPath}/ui?page=dispute&amp;state=resolved_payout"
                   class="btn btn-sm ${disputeState == 'resolved_payout' ? 'btn-dark' : 'btn-outline'}">
                    3B. Seller thắng → Giải ngân (resolved_payout)
                </a>
            </div>
        </section>

        <!-- HEADER PHÂN HỆ F9 DISPUTE & REFUND -->
        <section class="brutal-card">
            <div class="section-header" style="margin-bottom: 10px;">
                <div>
                    <span class="badge badge-verified" style="margin-bottom: 6px;">PHÂN HỆ F9 • PACKAGE: org.example.cunguoimoita.dispute</span>
                    <h1 class="section-title">⚖️ TRUNG TÂM KHIẾU NẠI &amp; BẢO VỆ HOÀN TIỀN (DISPUTE &amp; REFUND)</h1>
                    <p class="section-subtitle">
                         Quản lý vòng đời lớp <code>Dispute</code> (<code>openDispute</code>, <code>submitEvidence</code>, <code>resolveForBuyer</code>, <code>resolveForSeller</code>) và tự động sinh bản ghi <code>Refund</code>
                    </p>
                </div>
                <a href="${pageContext.request.contextPath}/ui?page=admin" class="btn btn-dark btn-sm">
                    🛡️ Sang góc nhìn Admin Trọng tài (F10)
                </a>
            </div>

            <!-- TIMELINE 5 TRẠNG THÁI DISPUTE -->
            <div class="step-timeline">
                <div class="step-node ${disputeState == 'open' ? 'active' : 'done'}">
                    <div style="font-size: 11px; color: #4b5563;">BƯỚC 1</div>
                    <div style="font-weight: 700;">Mở Khiếu Nại</div>
                    <code>status: open</code>
                </div>
                <div class="step-node ${disputeState == 'reviewing' ? 'active' : (disputeState == 'resolved_refund' or disputeState == 'resolved_payout' ? 'done' : '')}">
                    <div style="font-size: 11px; color: #4b5563;">BƯỚC 2</div>
                    <div style="font-weight: 700;">Nộp Bằng Chứng</div>
                    <code>status: reviewing</code>
                </div>
                <div class="step-node ${disputeState == 'resolved_refund' ? 'active' : ''}">
                    <div style="font-size: 11px; color: #4b5563;">PHÁN QUYẾT A</div>
                    <div style="font-weight: 700;">Hoàn Tiền Buyer</div>
                    <code>resolved_refund</code>
                </div>
                <div class="step-node ${disputeState == 'resolved_payout' ? 'active' : ''}">
                    <div style="font-size: 11px; color: #4b5563;">PHÁN QUYẾT B</div>
                    <div style="font-weight: 700;">Giải Ngân Seller</div>
                    <code>resolved_payout</code>
                </div>
                <div class="step-node">
                    <div style="font-size: 11px; color: #4b5563;">PHÁN QUYẾT C</div>
                    <div style="font-weight: 700;">Từ Chối KN</div>
                    <code>status: rejected</code>
                </div>
            </div>
        </section>

        <!-- BỐ CỤC 2 CỘT: ĐỐI CHIẾU BẰNG CHỨNG 2 BÊN (TRÁI) + FORM GỬI KHIẾU NẠI MỚI (PHẢI) -->
        <div class="split-2col">
            <!-- CỘT TRÁI: HỒ SƠ TRANH CHẤP ĐANG XỬ LÝ (#DSP-2026-041) -->
            <div class="brutal-card">
                <div class="section-header">
                    <div>
                        <span class="badge badge-danger">HỒ SƠ TRANH CHẤP #DSP-2026-041</span>
                        <h2 class="section-title" style="font-size: 17px; margin-top: 4px;">
                            Đơn hàng #ORD-2026-0889 • Lý do: <code>damaged</code> (Hàng hư hỏng)
                        </h2>
                    </div>
                    <span class="badge badge-swap">Đóng băng: 1.330.000 đ</span>
                </div>

                <!-- SO SÁNH BẰNG CHỨNG 2 BÊN (BUYER VS SELLER) -->
                <div class="split-equal" style="margin-bottom: 18px;">
                    <div style="background: #fee2e2; border: 1.5px solid #111; border-radius: 12px; padding: 14px;">
                        <div style="font-weight: 700; color: #991b1b; margin-bottom: 6px;">
                            🧑 NGƯỜI KHIẾU NẠI (<code>raisedById = #04 Tuấn Anh</code>)
                        </div>
                        <p style="font-size: 13px; margin: 0 0 10px;">
                            "Khi mở hộp có quay video unbox, phím Space và cụm phím mũi tên bị liệt switch không nhận tín hiệu dù bài đăng ghi hoạt động hoàn hảo 100%."
                        </p>
                        <div style="font-size: 11px; font-weight: 700; margin-bottom: 4px;">Bằng chứng đính kèm (<code>evidenceImages</code>):</div>
                        <div style="display: flex; gap: 6px; flex-wrap: wrap;">
                            <span class="badge badge-pending">📹 unbox_keychron_k2.mp4</span>
                            <span class="badge badge-pending">🖼️ switch_damaged_01.jpg</span>
                        </div>
                    </div>

                    <div style="background: #fef9c3; border: 1.5px solid #111; border-radius: 12px; padding: 14px;">
                        <div style="font-weight: 700; color: #854d0e; margin-bottom: 6px;">
                            🏪 PHẢN HỒI NGƯỜI BÁN (<code>againstId = #14 Minh Khoa</code>)
                        </div>
                        <p style="font-size: 13px; margin: 0 0 10px;">
                            "Lúc đóng gói gửi GHN mình đã bọc 2 lớp xốp nổ, có thể do vận chuyển đè mạnh làm cong chân Hotswap. Mình đồng ý nhận lại hàng hoàn tiền."
                        </p>
                        <div style="font-size: 11px; font-weight: 700; margin-bottom: 4px;">Bằng chứng phản hồi:</div>
                        <div style="display: flex; gap: 6px; flex-wrap: wrap;">
                            <span class="badge badge-pending">🖼️ packing_photo_ghn.jpg</span>
                        </div>
                    </div>
                </div>

                <!-- PHÁN QUYẾT CỦA ADMIN TRỌNG TÀI -->
                <c:if test="${disputeState == 'resolved_refund'}">
                    <div style="background: #dcfce7; border: 2px solid #16a34a; border-radius: 12px; padding: 14px; margin-bottom: 16px;">
                        <div style="font-weight: 700; color: #14532d;">
                            ✔ PHÁN QUYẾT TRỌNG TÀI (<code>handledByAdminId = #01</code> • <code>resolveForBuyer()</code>):
                        </div>
                        <div style="font-size: 13px; color: #065f46; margin-top: 4px;">
                            <code>resolutionNote</code>: "Video unbox hợp lệ, xác nhận lỗi phần cứng. Chấp thuận hoàn tiền 100% (1.330.000 đ) về ví VNPay của người mua thông qua lệnh <strong>Refund #RFD-2026-019</strong>."
                        </div>
                    </div>
                </c:if>

                <!-- BẢNG LỊCH SỬ HOÀN TIỀN (BẢNG REFUNDS TRONG 09_dispute_payout.sql) -->
                <h3 style="font-family: 'Bungee', sans-serif; font-size: 15px; margin: 18px 0 10px;">
                    💸 DANH SÁCH LỆNH HOÀN TIỀN (BẢNG REFUNDS - F9)
                </h3>
                <div class="table-wrapper">
                    <table class="brutal-table">
                        <thead>
                            <tr>
                                <th>Mã Refund</th>
                                <th>Đơn hàng</th>
                                <th>Payment Gốc</th>
                                <th>Lý do (RefundReason)</th>
                                <th>Số tiền hoàn</th>
                                <th>Trạng thái</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td><strong>#RFD-019</strong></td>
                                <td><code>ORD-2026-0889</code></td>
                                <td><code>PAY-99201</code></td>
                                <td><span class="badge badge-swap">dispute_won</span></td>
                                <td><strong>1.330.000 đ</strong></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${disputeState == 'resolved_refund'}">
                                            <span class="badge badge-sale">done (Đã hoàn)</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="badge badge-pending">pending (Chờ xử lý)</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                            </tr>
                            <tr>
                                <td><strong>#RFD-014</strong></td>
                                <td><code>ORD-2026-0752</code></td>
                                <td><code>PAY-99142</code></td>
                                <td><span class="badge badge-both">wrong_item</span></td>
                                <td><strong>520.000 đ</strong></td>
                                <td><span class="badge badge-sale">done (Đã hoàn)</span></td>
                            </tr>
                            <tr>
                                <td><strong>#RFD-008</strong></td>
                                <td><code>ORD-2026-0611</code></td>
                                <td><code>PAY-98990</code></td>
                                <td><span class="badge badge-pending">cancelled</span></td>
                                <td><strong>280.000 đ</strong></td>
                                <td><span class="badge badge-sale">done (Đã hoàn)</span></td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>

            <!-- CỘT PHẢI: FORM MỞ KHIẾU NẠI MỚI (Dispute.openDispute()) -->
            <aside class="brutal-card" style="background: #fffef8;">
                <h2 class="section-title" style="font-size: 17px; margin-bottom: 6px;">
                    📝 TẠO YÊU CẦU KHIẾU NẠI MỚI
                </h2>
                <p class="section-subtitle" style="margin-bottom: 16px;">
                    Gửi tới <code>DisputeServlet</code> để đóng băng lệnh giải ngân của đơn hàng và yêu cầu hoàn tiền
                </p>

                <form action="${pageContext.request.contextPath}/dispute/create" method="post">

                    <div class="form-group">
                        <label class="form-label">Mã đơn hàng cần khiếu nại (<code>orderId</code>)</label>
                        <select name="orderId" class="form-control">
                            <option value="889">ORD-2026-0889 — Bàn phím cơ Keychron K2 Pro (1.330.000 đ)</option>
                            <option value="902">ORD-2026-0902 — Máy ảnh Film Olympus OM-1 (2.800.000 đ)</option>
                        </select>
                    </div>

                    <div class="form-group">
                        <label class="form-label">Lý do khiếu nại (Enum <code>DisputeReason</code>)</label>
                        <select name="reason" class="form-control">
                            <option value="damaged">damaged — Hàng bị bể vỡ / hư hỏng chức năng</option>
                            <option value="wrong_item">wrong_item — Giao sai mẫu / khác mô tả bài đăng</option>
                            <option value="not_received">not_received — Đơn báo giao thành công nhưng chưa nhận được</option>
                            <option value="fake">fake — Phát hiện hàng giả / nhái thương hiệu</option>
                            <option value="other">other — Lý do tranh chấp khác</option>
                        </select>
                    </div>

                    <div class="form-group">
                        <label class="form-label">Mô tả chi tiết sự cố (<code>description</code>)</label>
                        <textarea name="description" rows="4" class="form-control"
                                  placeholder="Mô tả rõ tình trạng sản phẩm khi nhận hàng và phương án mong muốn..."></textarea>
                    </div>

                    <div class="form-group">
                        <label class="form-label">Đính kèm ảnh/video đồng kiểm (<code>evidenceImages</code>)</label>
                        <input type="file" multiple class="form-control" />
                        <span style="font-size: 11px; color: #4b5563;">
                            Hỗ trợ <code>@MultipartConfig</code> lưu đường dẫn bằng chứng dạng chuỗi phân tách dấu phẩy.
                        </span>
                    </div>

                    <button type="submit" class="btn btn-danger" style="width: 100%;">
                        🚨 Gửi Khiếu Nại &amp; Đóng Băng Tiền Escrow
                    </button>
                </form>
            </aside>
        </div>
    </main>
</div>

<jsp:include page="/WEB-INF/views/common/demo-footer.jsp" />
