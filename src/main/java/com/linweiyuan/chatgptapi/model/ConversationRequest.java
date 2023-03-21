package com.linweiyuan.chatgptapi.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ConversationRequest(
        String action,
        List<Message> messages,
        String model,
        @JsonProperty("parent_message_id")
        String parentMessageId,
        @JsonProperty("conversation_id")
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        String conversationId
) {
}