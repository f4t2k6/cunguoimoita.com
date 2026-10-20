package org.example.cunguoimoita.admin.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.cunguoimoita.dispute.model.Dispute;
import org.example.cunguoimoita.dispute.service.DisputeService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/** Admin xử lý khiếu nại (Admin.resolveDispute). */
@WebServlet(name = "AdminDisputeServlet", urlPatterns = {"/admin/disputes", "/admin/disputes/resolve"})
public class AdminDisputeServlet extends AdminBaseServlet {

    private final DisputeService disputeService = new DisputeService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            List<Dispute> pendingList = disputeService.listPendingForAdmin();
            List<Dispute> allList = disputeService.listAll(30);

            req.setAttribute("pendingDisputes", pendingList);
            req.setAttribute("allDisputes", allList);

            String selectedId = req.getParameter("id");
            if (selectedId != null && !selectedId.isBlank()) {
                req.setAttribute("selectedDispute", disputeService.getById(selectedId));
            } else if (!pendingList.isEmpty()) {
                req.setAttribute("selectedDispute", pendingList.get(0));
            }

        } catch (SQLException ex) {
            throw new ServletException(ex);
        }

        render(req, resp, "disputes.jsp", "disputes");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String action = req.getParameter("action");
        String disputeId = req.getParameter("disputeId");
        String note = req.getParameter("resolutionNote");
        String admin = adminId(req);

        handle(req, resp, "/admin/disputes", () -> {
            if ("resolve_buyer".equals(action)) {
                disputeService.resolveForBuyer(disputeId, admin, note != null ? note : "Chấp thuận khiếu nại của người mua, hoàn tiền 100%.");
                return "Đã phán quyết: Người mua thắng kiện. Tiền đã được tự động hoàn (Refund) về ví người mua.";
            } else if ("resolve_seller".equals(action)) {
                disputeService.resolveForSeller(disputeId, admin, note != null ? note : "Bác khiếu nại, giải ngân cho người bán.");
                return "Đã phán quyết: Người bán thắng kiện. Lệnh giải ngân (Payout) đã được kích hoạt cho người bán.";
            } else if ("reject".equals(action)) {
                disputeService.rejectDispute(disputeId, admin, note != null ? note : "Từ chối xử lý khiếu nại.");
                return "Đã từ chối xử lý khiếu nại.";
            } else if ("review".equals(action)) {
                disputeService.reviewDispute(disputeId, admin);
                return "Đã tiếp nhận hồ sơ khiếu nại vào trạng thái đang thẩm định.";
            }
            throw new IllegalArgumentException("Hành động trọng tài không hợp lệ: " + action);
        });
    }
}
