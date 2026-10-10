package org.example.cunguoimoita.shipping.model;

/** Class ShippingCarrier trong class diagram. */
public class ShippingCarrier {

    /** Phụ phí khi người bán và người mua khác tỉnh/thành (giả lập, không nối API thật). */
    public static final double INTER_CITY_SURCHARGE = 15000;

    private String id;
    private String name;          // GHN, GHTK, Ahamove, Viettel Post
    private String logoUrl;
    private String hotline;
    private double baseFee;
    private int estimatedDays;
    private boolean isActive;

    public ShippingCarrier() {
    }

    public ShippingCarrier(String id, String name, String logoUrl, String hotline,
                           double baseFee, int estimatedDays, boolean isActive) {
        this.id = id;
        this.name = name;
        this.logoUrl = logoUrl;
        this.hotline = hotline;
        this.baseFee = baseFee;
        this.estimatedDays = estimatedDays;
        this.isActive = isActive;
    }

    /** + calculateFee(fromCity, toCity): double */
    public double calculateFee(String fromCity, String toCity) {
        boolean sameCity = fromCity != null && fromCity.trim().equalsIgnoreCase(toCity == null ? "" : toCity.trim());
        return sameCity ? baseFee : baseFee + INTER_CITY_SURCHARGE;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    public String getHotline() { return hotline; }
    public void setHotline(String hotline) { this.hotline = hotline; }
    public double getBaseFee() { return baseFee; }
    public void setBaseFee(double baseFee) { this.baseFee = baseFee; }
    public int getEstimatedDays() { return estimatedDays; }
    public void setEstimatedDays(int estimatedDays) { this.estimatedDays = estimatedDays; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
