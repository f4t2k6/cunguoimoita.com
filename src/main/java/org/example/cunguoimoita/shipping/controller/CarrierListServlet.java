package org.example.cunguoimoita.shipping.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.cunguoimoita.shipping.service.ShippingService;

import java.io.IOException;
import java.sql.SQLException;

/** Tra cứu đơn vị vận chuyển và phí theo tuyến. */
@WebServlet("/shipping/carriers")
public class CarrierListServlet extends HttpServlet {

    private final ShippingService shippingService = new ShippingService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            req.setAttribute("carriers", shippingService.listCarriers());
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        req.setAttribute("from", req.getParameter("from"));
        req.setAttribute("to", req.getParameter("to"));
        req.getRequestDispatcher("/WEB-INF/views/shipping/carriers.jsp").forward(req, resp);
    }
}
