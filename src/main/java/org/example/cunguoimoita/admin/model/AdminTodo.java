package org.example.cunguoimoita.admin.model;

/** Số việc admin đang chờ xử lý, hiện trên menu của mọi trang /admin/*. */
public record AdminTodo(int pendingProducts, int pendingReports, int openDisputes) {
    public int getPendingProducts() { return pendingProducts; }
    public int getPendingReports() { return pendingReports; }
    public int getOpenDisputes() { return openDisputes; }
}
