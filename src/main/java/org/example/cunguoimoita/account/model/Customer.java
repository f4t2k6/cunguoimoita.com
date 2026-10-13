package org.example.cunguoimoita.account.model;

import org.example.cunguoimoita.common.model.AccountStatus;
import org.example.cunguoimoita.common.model.UserRole;

import java.time.OffsetDateTime;

public class Customer extends User {
    private final String tierId;
    private final String payoutAccountRef;

    public Customer(String id, String email, String passwordHash, UserRole role,
                    AccountStatus status, String fullName, String phone,
                    String avatarPath, OffsetDateTime createdAt,
                    String tierId, String payoutAccountRef) {
        super(id, email, passwordHash, role, status, fullName, phone, avatarPath, createdAt);
        this.tierId = tierId;
        this.payoutAccountRef = payoutAccountRef;
    }

    public String getTierId() {
        return tierId;
    }

    public String getPayoutAccountRef() {
        return payoutAccountRef;
    }
}
