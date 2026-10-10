package org.example.cunguoimoita.notification.model;

/** Notification.type: enum {order, payment, shipment, review, system} */
public enum NotificationType {
    ORDER, PAYMENT, SHIPMENT, REVIEW, SYSTEM;

    public String dbValue() {
        return name().toLowerCase();
    }

    public static NotificationType fromDb(String value) {
        return valueOf(value.toUpperCase());
    }
}
