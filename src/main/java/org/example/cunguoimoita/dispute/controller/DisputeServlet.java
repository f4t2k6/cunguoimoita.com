package org.example.cunguoimoita.dispute.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.cunguoimoita.common.util.CurrentUser;
import org.example.cunguoimoita.dispute.model.Dispute;
import org.example.cunguoimoita.dispute.model.DisputeReason;
import org.example.cunguoimoita.dispute.service.DisputeService;
import org.example.cunguoimoita.dispute.service.RefundService;
import org.example.cunguoimoita.order.model.Order;
import org.example.cunguoimoita.order.service.OrderService;
import org.example.cunguoimoita.common.util.MediaStore;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;

/** Khiếu nại và hoàn tiền phía người dùng: người mua mở khiếu nại, người bán phản hồi, người mua nộp thêm bằng chứng. */
@WebServlet(name = "DisputeServlet", urlPatterns = {"/dispute", "/dispute/create", "/dispute/respond", "/dispute/evidence"})
@MultipartConfig(maxFileSize = MediaStore.MAX_FILE_SIZE, maxRequestSize = 6 * MediaStore.MAX_FILE_SIZE)
public class DisputeServlet extends HttpServlet {

    private static final int MAX_IMAGES = 5;

    private final DisputeService disputeService = new DisputeService();
    private final RefundService refundService = new RefundService();
    private final OrderService orderService = new OrderService();
    private final MediaStore mediaStore = new MediaStore();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String userId = CurrentUser.id(req);
        if (userId == null) {
            CurrentUser.redirectToLogin(req, resp);
            return;
        }
        req.setAttribute("userId", userId);
        req.setAttribute("error", req.getParameter("error"));
        req.setAttribute("message", req.getParameter("message"));
        req.setAttribute("reasons", DisputeReason.values());
        try {
            List<Dispute> userDisputes = disputeService.getByUser(userId);
            req.setAttribute("userDisputes", userDisputes);
            req.setAttribute("userRefunds", refundService.getByBuyerId(userId));

            String disputeId = req.getParameter("id");
            String orderId = req.getParameter("orderId");
            if (disputeId != null && !disputeId.isBlank()) {
                Dispute d = disputeService.getForUser(disputeId, userId, CurrentUser.isAdmin(req));
                req.setAttribute("currentDispute", d);
                req.setAttribute("order", orderService.getOrder(d.getOrderId(), d.getRaisedById()));
            } else if (orderId != null && !orderId.isBlank()) {
                Order order = orderService.getOrder(orderId, userId);
                req.setAttribute("order", order);
                req.setAttribute("openProblem", disputeService.openProblem(orderId, userId));
            }
        } catch (IllegalArgumentException | SecurityException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        } catch (SQLException ex) {
            throw new ServletException(ex);
        }
        req.getRequestDispatcher("/WEB-INF/views/dispute/dispute-center.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String userId = CurrentUser.id(req);
        if (userId == null) {
            CurrentUser.redirectToLogin(req, resp);
            return;
        }
        String path = req.getServletPath();
        String disputeId = req.getParameter("disputeId");
        String orderId = req.getParameter("orderId");
        // Lỗi thì quay lại đúng trang đang mở (form mở khiếu nại hoặc chi tiết khiếu nại)
        String back = "/dispute/create".equals(path) ? "/dispute?orderId=" + encode(orderId) : "/dispute?id=" + encode(disputeId);
        try {
            String images = String.join(",", mediaStore.saveAll(req.getParts(), "images", MAX_IMAGES, userId));
            switch (path) {
                case "/dispute/create": {
                    Dispute d = disputeService.openDispute(orderId, userId, DisputeReason.fromDb(req.getParameter("reason")),
                            req.getParameter("description"), images.isEmpty() ? null : images);
                    back = "/dispute?id=" + encode(d.getId());
                    break;
                }
                case "/dispute/respond":
                    disputeService.respond(disputeId, userId, req.getParameter("response"), images);
                    break;
                case "/dispute/evidence":
                    disputeService.submitEvidence(disputeId, userId, images);
                    break;
                default:
                    throw new IllegalArgumentException("Thao tác không hợp lệ");
            }
            resp.sendRedirect(req.getContextPath() + back + "&message=" + encode("Đã gửi"));
        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            resp.sendRedirect(req.getContextPath() + back + "&error=" + encode(e.getMessage()));
        } catch (SQLException ex) {
            throw new ServletException(ex);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
