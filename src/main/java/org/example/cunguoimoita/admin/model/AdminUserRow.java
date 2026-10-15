package org.example.cunguoimoita.admin.model;

import java.time.LocalDate;

/** Một dòng người dùng trên trang quản lý người dùng. */
public record AdminUserRow(String id, String fullname, String email, String phone, String role, String status,
                           LocalDate joinDate, String tierName, int productCount, int reportCount) {

    public boolean isBanned() { return "banned".equals(status); }
    public boolean isAdmin() { return "admin".equals(role); }

    public String getId() { return id; }
    public String getFullname() { return fullname; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getRole() { return role; }
    public String getStatus() { return status; }
    public LocalDate getJoinDate() { return joinDate; }
    public String getTierName() { return tierName; }
    public int getProductCount() { return productCount; }
    public int getReportCount() { return reportCount; }

    // EL của Tomcat đọc record qua tên thành phần (vd. ${u.banned} gọi banned()), nên thêm cả dạng này
    public boolean banned() { return isBanned(); }
    public boolean admin() { return isAdmin(); }
}
