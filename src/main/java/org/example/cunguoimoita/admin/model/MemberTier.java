package org.example.cunguoimoita.admin.model;

/** Class MemberTier trong class diagram (Bronze, Silver, Gold, Diamond). */
public class MemberTier {

    private String id;
    private String name;
    private int minEcoPoints;
    private double discountRate;    // 0.03 = giảm 3%
    private String benefitNote;
    private int memberCount;        // chỉ để hiển thị

    /** discountRate dạng phần trăm để hiện trên form (0.03 → 3). */
    public double getDiscountPercent() { return Math.round(discountRate * 10000) / 100.0; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getMinEcoPoints() { return minEcoPoints; }
    public void setMinEcoPoints(int minEcoPoints) { this.minEcoPoints = minEcoPoints; }
    public double getDiscountRate() { return discountRate; }
    public void setDiscountRate(double discountRate) { this.discountRate = discountRate; }
    public String getBenefitNote() { return benefitNote; }
    public void setBenefitNote(String benefitNote) { this.benefitNote = benefitNote; }
    public int getMemberCount() { return memberCount; }
    public void setMemberCount(int memberCount) { this.memberCount = memberCount; }
}
