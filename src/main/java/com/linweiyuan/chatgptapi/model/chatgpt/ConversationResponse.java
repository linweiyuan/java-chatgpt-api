package com.linweiyuan.chatgptapi.model.chatgpt;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ConversationResponse(
        @JsonProperty("message")
        ConversationResponseMessage conversationResponseMessage
) {
}
