package org.example.cunguoimoita.admin.model;

/**
 * Số việc admin đang phải xử lý, hiện thành số nhỏ trên thanh menu của mọi trang /admin/*.
 * JSP đọc ${adminTodo.pendingProducts}... (EL của Tomcat 11 đọc record theo tên thành phần).
 */
public record AdminTodo(int pendingProducts, int pendingReports, int openDisputes) {
    public int getPendingProducts() { return pendingProducts; }
    public int getPendingReports() { return pendingReports; }
    public int getOpenDisputes() { return openDisputes; }
}
