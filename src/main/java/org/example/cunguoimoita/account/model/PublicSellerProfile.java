package org.example.cunguoimoita.account.model;

public class PublicSellerProfile {
    private final String userId;
    private final String fullName;
    private final String avatarPath;
    private final double averageRating;
    private final long totalSales;
    private final String memberTier;

    public PublicSellerProfile(String userId, String fullName, String avatarPath,
                               double averageRating, long totalSales, String memberTier) {
        this.userId = userId;
        this.fullName = fullName;
        this.avatarPath = avatarPath;
        this.averageRating = averageRating;
        this.totalSales = totalSales;
        this.memberTier = memberTier;
    }

    public String getUserId() { return userId; }
    public String getFullName() { return fullName; }
    public String getAvatarPath() { return avatarPath; }
    public double getAverageRating() { return averageRating; }
    public long getTotalSales() { return totalSales; }
    public String getMemberTier() { return memberTier; }
}
