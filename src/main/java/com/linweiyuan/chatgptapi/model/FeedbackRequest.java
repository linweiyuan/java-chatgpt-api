package com.linweiyuan.chatgptapi.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FeedbackRequest(
        @JsonProperty("message_id")
        String messageId,
        @JsonProperty("conversation_id")
        String conversationId,
        String rating
) {
}