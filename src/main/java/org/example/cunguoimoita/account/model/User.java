package org.example.cunguoimoita.account.model;

import org.example.cunguoimoita.common.model.AccountStatus;
import org.example.cunguoimoita.common.model.UserRole;

import java.time.OffsetDateTime;

public class User {
    private final String id;
    private final String email;
    private final String passwordHash;
    private final UserRole role;
    private final AccountStatus status;
    private final String fullName;
    private final String phone;
    private final String avatarPath;
    private final OffsetDateTime createdAt;

    public User(String id, String email, String passwordHash, UserRole role,
                AccountStatus status, String fullName, String phone,
                String avatarPath, OffsetDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status;
        this.fullName = fullName;
        this.phone = phone;
        this.avatarPath = avatarPath;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public UserRole getRole() { return role; }
    public AccountStatus getStatus() { return status; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public String getAvatarPath() { return avatarPath; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
