package org.example.cunguoimoita.chat.model;

/** PriceOffer.status: enum {pending, accepted, declined, expired} */
public enum OfferStatus {
    PENDING, ACCEPTED, DECLINED, EXPIRED;

    public String dbValue() {
        return name().toLowerCase();
    }

    public static OfferStatus fromDb(String value) {
        return valueOf(value.toUpperCase());
    }
}
