package org.example.cunguoimoita.chat.model;

/** Message.type: enum {text, image, price_offer} */
public enum MessageType {
    TEXT, IMAGE, PRICE_OFFER;

    public String dbValue() {
        return name().toLowerCase();
    }

    public static MessageType fromDb(String value) {
        return valueOf(value.toUpperCase());
    }
}
