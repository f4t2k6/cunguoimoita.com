package org.example.cunguoimoita.admin.model;

import org.example.cunguoimoita.order.model.Order;

/** Số đơn theo từng trạng thái (trang tổng quan admin). JSP đọc ${s.count} và ${s.status.label()}. */
public record OrderStatusCount(Order.Status status, int count) {
    public Order.Status getStatus() { return status; }
    public int getCount() { return count; }
}
