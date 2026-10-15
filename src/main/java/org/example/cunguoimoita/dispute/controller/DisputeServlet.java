package org.example.cunguoimoita.dispute.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.cunguoimoita.common.dao.OrderRefDAO;
import org.example.cunguoimoita.common.db.ConnectionPool;
import org.example.cunguoimoita.common.util.CurrentUser;
import org.example.cunguoimoita.dispute.model.Dispute;
import org.example.cunguoimoita.dispute.model.DisputeReason;
import org.example.cunguoimoita.dispute.service.DisputeService;
import org.example.cunguoimoita.dispute.service.RefundService;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Controller cho Người dùng xem và tạo yêu cầu Khiếu nại & Tra cứu Hoàn tiền (F9 Dispute & Refund).
 */
@WebServlet(name = "DisputeServlet", urlPatterns = {"/dispute", "/dispute/create"})
public class DisputeServlet extends HttpServlet {

    private final DisputeService disputeService = new DisputeService();
    private final RefundService refundService = new RefundService();
    private final OrderRefDAO orderDAO = new OrderRefDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String userId = CurrentUser.id(req);
        if (userId == null) {
            userId = "B1"; // Mặc định người mua mẫu
        }

        try {
            List<Dispute> userDisputes = disputeService.getByUser(userId);
            req.setAttribute("userDisputes", userDisputes);
            req.setAttribute("userRefunds", refundService.getByBuyerId(userId));

            String disputeId = req.getParameter("id");
            if (disputeId != null && !disputeId.isBlank()) {
                req.setAttribute("currentDispute", disputeService.getById(disputeId));
            } else if (!userDisputes.isEmpty()) {
                req.setAttribute("currentDispute", userDisputes.get(0));
            }

            // Kịch bản demo trên lớp
            String state = req.getParameter("state");
            if (state != null && !state.isBlank()) {
                req.setAttribute("disputeState", state);
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            req.setAttribute("error", ex.getMessage());
        }

        req.setAttribute("currentPage", "dispute");
        req.getRequestDispatcher("/WEB-INF/views/dispute/dispute-center.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String userId = CurrentUser.id(req);
        if (userId == null) {
            userId = "B1";
        }

        String orderId = req.getParameter("orderId");
        String reasonStr = req.getParameter("reason");
        String description = req.getParameter("description");
        String evidence = req.getParameter("evidenceImages");

        if (orderId == null || orderId.isBlank()) {
            orderId = "O1";
        }
        if (description == null || description.isBlank()) {
            description = "Sản phẩm không đúng cam kết ban đầu.";
        }

        DisputeReason reason = DisputeReason.fromDb(reasonStr);

        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            OrderRefDAO.OrderRef order = orderDAO.find(c, orderId);
            String againstId = order != null ? order.sellerId() : "S2";

            Dispute d = disputeService.openDispute(orderId, userId, againstId, reason, description, evidence != null ? evidence : "photo_unbox.jpg");
            resp.sendRedirect(req.getContextPath() + "/dispute?id=" + d.getId() + "&flash=success");

        } catch (SQLException ex) {
            throw new ServletException(ex);
        }
    }
}
