package org.example.cunguoimoita.shipping.model;

/** Shipment.codStatus: enum {none, pending, collected, remitted} */
public enum CodStatus {
    NONE, PENDING, COLLECTED, REMITTED;

    public String dbValue() {
        return name().toLowerCase();
    }

    public static CodStatus fromDb(String value) {
        return valueOf(value.toUpperCase());
    }
}
