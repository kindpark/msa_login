package com.example.login.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserDeletedEventMessage {

    private final String eventId;
    private final String type;
    private final Long actorId;

    @JsonCreator
    private UserDeletedEventMessage(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("type") String type,
            @JsonProperty("actorId") Long actorId
    ) {
        this.eventId = eventId;
        this.type = type;
        this.actorId = actorId;
    }
}
