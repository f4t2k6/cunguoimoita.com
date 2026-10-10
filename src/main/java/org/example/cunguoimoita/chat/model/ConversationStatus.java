package org.example.cunguoimoita.chat.model;

/** Conversation.status: enum {open, closed} */
public enum ConversationStatus {
    OPEN, CLOSED;

    public String dbValue() {
        return name().toLowerCase();
    }

    public static ConversationStatus fromDb(String value) {
        return valueOf(value.toUpperCase());
    }
}
