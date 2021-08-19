package com.cms.vacations.messages;

import com.cms.vacations.SerializableMessage;

import java.util.UUID;

public class MessageEnvelope implements SerializableMessage {

    private final VacationMessage message;
    private final String to;

    public VacationMessage getMessage() {
        return message;
    }

    public String getTo() {
        return to;
    }

    public MessageEnvelope(VacationMessage message) {
        this(message, UUID.randomUUID().toString());
    }

    public MessageEnvelope(VacationMessage message, String to) {
        this.message = message;
        this.to = to;
    }
}
