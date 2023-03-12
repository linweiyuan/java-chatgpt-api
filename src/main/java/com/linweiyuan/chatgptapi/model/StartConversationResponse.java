package com.linweiyuan.chatgptapi.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StartConversationResponse(
        Message message,
        @JsonProperty("conversation_id")
        String conversationId
) {
}
