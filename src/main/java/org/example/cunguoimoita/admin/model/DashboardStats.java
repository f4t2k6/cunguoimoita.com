package org.example.cunguoimoita.admin.model;

/** Số liệu trang tổng quan admin. */
public record DashboardStats(int customers, int bannedUsers, int products, int availableProducts,
                             int pendingProducts, int pendingReports, int orders, int openDisputes,
                             double gmv, double commission, int newCustomers7d, int orders7d) {

    public int getCustomers() { return customers; }
    public int getBannedUsers() { return bannedUsers; }
    public int getProducts() { return products; }
    public int getAvailableProducts() { return availableProducts; }
    public int getPendingProducts() { return pendingProducts; }
    public int getPendingReports() { return pendingReports; }
    public int getOrders() { return orders; }
    public int getOpenDisputes() { return openDisputes; }
    public double getGmv() { return gmv; }
    public double getCommission() { return commission; }
    /** Khách đăng ký và đơn tạo trong 7 ngày gần nhất. */
    public int getNewCustomers7d() { return newCustomers7d; }
    public int getOrders7d() { return orders7d; }
}
