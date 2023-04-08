package com.linweiyuan.chatgptapi.model.chatgpt;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ConversationResponseMessage(
        String id,
        Content content,
        @JsonProperty("end_turn")
        boolean endTurn,
        Metadata metadata
) {
}
