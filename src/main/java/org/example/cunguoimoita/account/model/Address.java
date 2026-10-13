package org.example.cunguoimoita.account.model;

import java.time.OffsetDateTime;

public class Address {
    private final String id;
    private final String userId;
    private final String label;
    private final String recipientName;
    private final String phone;
    private final String addressLine;
    private final String ward;
    private final String district;
    private final String province;
    private final boolean defaultAddress;
    private final OffsetDateTime createdAt;

    public Address(String id, String userId, String label, String recipientName,
                   String phone, String addressLine, String ward, String district,
                   String province, boolean defaultAddress, OffsetDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.label = label;
        this.recipientName = recipientName;
        this.phone = phone;
        this.addressLine = addressLine;
        this.ward = ward;
        this.district = district;
        this.province = province;
        this.defaultAddress = defaultAddress;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getLabel() { return label; }
    public String getRecipientName() { return recipientName; }
    public String getPhone() { return phone; }
    public String getAddressLine() { return addressLine; }
    public String getWard() { return ward; }
    public String getDistrict() { return district; }
    public String getProvince() { return province; }
    public boolean isDefaultAddress() { return defaultAddress; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
