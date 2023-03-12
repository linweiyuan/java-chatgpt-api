package com.linweiyuan.chatgptapi.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StartConversationRequest(
        @JsonProperty("conversation_id")
        String conversationId,
        @JsonProperty("parent_message_id")
        String parentMessageId,
        @JsonProperty("message_id")
        String messageId,
        String content
) {
}
